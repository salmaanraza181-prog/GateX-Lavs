const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProjectPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    name: "Half Adder Lab",
    description: "XOR and AND combinational circuit",
    activePageId: "p1",
    pages: [
      {
        id: "p1",
        name: "Sheet 1",
        components: [{ id: "g1", type: "GATE_XOR", label: "U1", x: 6, y: 4 }],
        wires: [],
        junctions: [],
      },
    ],
    componentCount: 1,
    wireCount: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validCustomIcPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    name: "Half Adder Chip",
    shortCode: "HADD",
    description: "2-input sum and carry chip",
    inputLabels: ["A", "B"],
    outputLabels: ["SUM", "COUT"],
    truthEntries: ["0:0", "1:1", "2:1", "3:2"],
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write cloud projects or custom ICs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .set(validProjectPayload(ALICE_UID))
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("custom_ics")
      .doc("ic_1")
      .set(validCustomIcPayload(ALICE_UID))
  );
});

test("Authenticated owner can create, list, partially update, and delete own projects", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_half_adder");

  await assertSucceeds(ref.set(validProjectPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertSucceeds(ref.get());
  await assertSucceeds(
    ref.update({
      name: "Half Adder Lab v2",
      componentCount: 3,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(ref.delete());
});

test("Cross-user isolation blocks Bob from reading, listing, modifying, or deleting Alice's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .set(validProjectPayload(ALICE_UID));
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("custom_ics")
      .doc("ic_1")
      .set(validCustomIcPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").update({
      name: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").delete());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("custom_ics").doc("ic_1").get());
});

test("Shadow fields, @DocumentId id key, immutable createdAt mutation, and future timestamps are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("projects");

  // Extra ghost field
  await assertFails(
    col.doc("proj_ghost").set(validProjectPayload(ALICE_UID, { isAdmin: true }))
  );

  // @DocumentId 'id' stored as payload field must fail
  await assertFails(
    col.doc("proj_with_id").set(validProjectPayload(ALICE_UID, { id: "proj_with_id" }))
  );

  // Future timestamp rejected
  await assertFails(
    col
      .doc("proj_future")
      .set(validProjectPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) }))
  );

  // Immutable createdAt change rejected on update
  await assertSucceeds(col.doc("proj_ok").set(validProjectPayload(ALICE_UID)));
  await assertFails(
    col.doc("proj_ok").update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Authenticated owner can create, update, list, and delete Custom ICs with strict bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const icRef = aliceDb.collection("users").doc(ALICE_UID).collection("custom_ics").doc("ic_hadd_1");

  await assertSucceeds(icRef.set(validCustomIcPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("custom_ics").get());
  await assertSucceeds(
    icRef.update({
      description: "Verified 2-input adder",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Over-sized inputLabels (> 4) must fail
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("custom_ics")
      .doc("ic_bad")
      .set(validCustomIcPayload(ALICE_UID, { inputLabels: ["A", "B", "C", "D", "E"] }))
  );

  await assertSucceeds(icRef.delete());
});
