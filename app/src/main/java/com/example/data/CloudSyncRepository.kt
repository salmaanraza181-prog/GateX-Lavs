package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.model.CircuitPage
import com.example.model.CircuitProject
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.CustomIcDefinition
import com.example.model.GridPoint
import com.example.model.WireEndpoint
import com.example.model.WireJunction
import com.example.model.WireSegment
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

object CloudAuthManager {
    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onUnauthenticated()
            return
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        scope.launch {
            try {
                val cred = credentialManager.getCredential(context, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }
        val activity = context as? Activity
        if (activity == null) {
            onAuthError("Unable to launch Google Sign-In outside an Activity context")
            return
        }
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()
        scope.launch {
            try {
                val cred = credentialManager.getCredential(activity, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential type")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w("Auth", "Failed to clear credential state: ${e.message}")
                } finally {
                    Firebase.auth.signOut()
                    onSignOutComplete()
                }
            }
        }
    }
}

@Keep
@IgnoreExtraProperties
data class CloudProjectDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val activePageId: String = "p1",
    val pages: List<Map<String, Any>> = emptyList(),
    val componentCount: Long = 0L,
    val wireCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudCustomIcDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val shortCode: String = "",
    val description: String = "",
    val inputLabels: List<String> = emptyList(),
    val outputLabels: List<String> = emptyList(),
    val truthEntries: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class CloudSyncRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("Please sign in with Google to use Cloud Sync.")
        }
        return uid
    }

    private fun sanitizeDocId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        return cleaned.ifBlank { "doc_${System.currentTimeMillis()}" }
    }

    fun observeCloudProjects(): Flow<List<CircuitProject>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/projects"
        emitAll(
            db.collection("users").document(uid).collection("projects")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        val model = doc.toObject(
                            CloudProjectDocument::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: return@mapNotNull null
                        cloudDocToProject(model)
                    }.sortedByDescending { it.updatedAt }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeCloudCustomIcs(): Flow<List<CustomIcDefinition>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/custom_ics"
        emitAll(
            db.collection("users").document(uid).collection("custom_ics")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        val model = doc.toObject(
                            CloudCustomIcDocument::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: return@mapNotNull null
                        CustomIcDefinition(
                            id = model.id,
                            name = model.name.ifBlank { "Custom IC" },
                            shortCode = model.shortCode.ifBlank { "CIC" },
                            description = model.description,
                            inputLabels = model.inputLabels.ifEmpty { listOf("A", "B") },
                            outputLabels = model.outputLabels.ifEmpty { listOf("Y") },
                            truthMap = CustomIcStore.parseTruthEntries(model.truthEntries)
                        )
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun uploadProject(project: CircuitProject): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId(project.id)
            val docRef = db.collection("users").document(uid).collection("projects").document(docId)
            docPath = docRef.path

            val safeName = project.name.trim().ifBlank { "Untitled Circuit" }.take(120)
            val safeDesc = project.description.trim().take(500)
            val safeActivePage = project.activePageId.trim().ifBlank { "p1" }.take(64)
            val pageMaps = project.pages.take(20).ifEmpty { listOf(CircuitPage("p1", "Sheet 1")) }
                .map { pageToMap(it) }
            val compCount = project.pages.sumOf { it.components.size }.coerceIn(0, 2000).toLong()
            val wireCount = project.pages.sumOf { it.wires.size }.coerceIn(0, 4000).toLong()

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updatePayload = mapOf(
                    "name" to safeName,
                    "description" to safeDesc,
                    "activePageId" to safeActivePage,
                    "pages" to pageMaps,
                    "componentCount" to compCount,
                    "wireCount" to wireCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "name" to safeName,
                    "description" to safeDesc,
                    "activePageId" to safeActivePage,
                    "pages" to pageMaps,
                    "componentCount" to compCount,
                    "wireCount" to wireCount,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudProject(projectId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/$projectId"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId(projectId)
            val docRef = db.collection("users").document(uid).collection("projects").document(docId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun uploadCustomIc(ic: CustomIcDefinition): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/custom_ics"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId(ic.id)
            val docRef = db.collection("users").document(uid).collection("custom_ics").document(docId)
            docPath = docRef.path

            val safeName = ic.name.trim().ifBlank { "Custom IC" }.take(80)
            val safeCode = ic.shortCode.trim().ifBlank { "CIC" }.take(16)
            val safeDesc = ic.description.trim().take(300)
            val safeIn = ic.inputLabels.take(4).ifEmpty { listOf("A") }
            val safeOut = ic.outputLabels.take(4).ifEmpty { listOf("Y") }
            val safeTruth = CustomIcStore.truthMapToEntries(ic.truthMap).take(16).ifEmpty { listOf("0:0") }

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "name" to safeName,
                        "shortCode" to safeCode,
                        "description" to safeDesc,
                        "inputLabels" to safeIn,
                        "outputLabels" to safeOut,
                        "truthEntries" to safeTruth,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to safeName,
                        "shortCode" to safeCode,
                        "description" to safeDesc,
                        "inputLabels" to safeIn,
                        "outputLabels" to safeOut,
                        "truthEntries" to safeTruth,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudCustomIc(icId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/custom_ics/$icId"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId(icId)
            val docRef = db.collection("users").document(uid).collection("custom_ics").document(docId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun pageToMap(page: CircuitPage): Map<String, Any> = mapOf(
        "id" to page.id,
        "name" to page.name,
        "components" to page.components.map { c ->
            mapOf(
                "id" to c.id,
                "type" to c.type.name,
                "label" to c.label,
                "x" to c.x.toDouble(),
                "y" to c.y.toDouble(),
                "rotationDegrees" to c.rotationDegrees,
                "flipped" to c.flipped,
                "value" to c.value,
                "properties" to c.properties
            )
        },
        "wires" to page.wires.map { w ->
            buildMap<String, Any> {
                put("id", w.id)
                put("fromComp", w.from.componentId ?: "")
                put("fromPin", w.from.pinId ?: "")
                put("fromJunc", w.from.junctionId ?: "")
                put("toComp", w.to.componentId ?: "")
                put("toPin", w.to.pinId ?: "")
                put("toJunc", w.to.junctionId ?: "")
                put("waypoints", w.waypoints.map { pt -> mapOf("x" to pt.x.toDouble(), "y" to pt.y.toDouble()) })
                if (w.colorHex != null) put("colorHex", w.colorHex)
            }
        },
        "junctions" to page.junctions.map { j ->
            mapOf("id" to j.id, "x" to j.x.toDouble(), "y" to j.y.toDouble())
        }
    )

    @Suppress("UNCHECKED_CAST")
    private fun cloudDocToProject(doc: CloudProjectDocument): CircuitProject {
        val parsedPages = doc.pages.mapIndexed { idx, pMap ->
            val pId = (pMap["id"] as? String)?.ifBlank { "p${idx + 1}" } ?: "p${idx + 1}"
            val pName = (pMap["name"] as? String)?.ifBlank { "Sheet ${idx + 1}" } ?: "Sheet ${idx + 1}"

            val compsRaw = (pMap["components"] as? List<*>) ?: emptyList<Any>()
            val comps = compsRaw.mapNotNull { item ->
                val m = item as? Map<*, *> ?: return@mapNotNull null
                val typeName = m["type"] as? String ?: return@mapNotNull null
                val type = enumValues<ComponentType>().firstOrNull { it.name == typeName } ?: return@mapNotNull null
                val propsRaw = m["properties"] as? Map<*, *>
                val props = propsRaw?.entries?.associate { (k, v) -> k.toString() to (v?.toString() ?: "") } ?: emptyMap()
                ComponentInstance(
                    id = (m["id"] as? String) ?: "c_${System.nanoTime()}",
                    type = type,
                    label = (m["label"] as? String) ?: type.name,
                    x = (m["x"] as? Number)?.toFloat() ?: 4f,
                    y = (m["y"] as? Number)?.toFloat() ?: 4f,
                    rotationDegrees = (m["rotationDegrees"] as? Number)?.toInt() ?: 0,
                    flipped = (m["flipped"] as? Boolean) ?: false,
                    value = (m["value"] as? String) ?: "",
                    properties = props
                )
            }

            val wiresRaw = (pMap["wires"] as? List<*>) ?: emptyList<Any>()
            val wires = wiresRaw.mapNotNull { item ->
                val m = item as? Map<*, *> ?: return@mapNotNull null
                val fromComp = (m["fromComp"] as? String)?.ifBlank { null }
                val fromPin = (m["fromPin"] as? String)?.ifBlank { null }
                val fromJunc = (m["fromJunc"] as? String)?.ifBlank { null }
                val toComp = (m["toComp"] as? String)?.ifBlank { null }
                val toPin = (m["toPin"] as? String)?.ifBlank { null }
                val toJunc = (m["toJunc"] as? String)?.ifBlank { null }
                val wpsRaw = (m["waypoints"] as? List<*>) ?: emptyList<Any>()
                val wps = wpsRaw.mapNotNull { wp ->
                    val wm = wp as? Map<*, *> ?: return@mapNotNull null
                    val wx = (wm["x"] as? Number)?.toFloat() ?: return@mapNotNull null
                    val wy = (wm["y"] as? Number)?.toFloat() ?: return@mapNotNull null
                    GridPoint(wx, wy)
                }
                WireSegment(
                    id = (m["id"] as? String) ?: "w_${System.nanoTime()}",
                    from = WireEndpoint(fromComp, fromPin, fromJunc),
                    to = WireEndpoint(toComp, toPin, toJunc),
                    waypoints = wps,
                    colorHex = (m["colorHex"] as? Number)?.toLong()
                )
            }

            val juncsRaw = (pMap["junctions"] as? List<*>) ?: emptyList<Any>()
            val juncs = juncsRaw.mapNotNull { item ->
                val m = item as? Map<*, *> ?: return@mapNotNull null
                WireJunction(
                    id = (m["id"] as? String) ?: "j_${System.nanoTime()}",
                    x = (m["x"] as? Number)?.toFloat() ?: 0f,
                    y = (m["y"] as? Number)?.toFloat() ?: 0f
                )
            }
            CircuitPage(pId, pName, comps, wires, juncs)
        }.ifEmpty { listOf(CircuitPage("p1", "Sheet 1")) }

        val createdMs = doc.createdAt?.toDate()?.time ?: System.currentTimeMillis()
        val updatedMs = doc.updatedAt?.toDate()?.time ?: createdMs
        return CircuitProject(
            id = doc.id,
            name = doc.name.ifBlank { "Cloud Circuit" },
            description = doc.description,
            createdAt = createdMs,
            updatedAt = updatedMs,
            pages = parsedPages,
            activePageId = doc.activePageId.ifBlank { parsedPages.first().id }
        )
    }
}
