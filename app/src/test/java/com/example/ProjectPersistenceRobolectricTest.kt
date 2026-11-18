package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.CircuitTemplates
import com.example.data.ProjectRepository
import com.example.data.ProjectSerializer
import com.example.data.SalmanDatabase
import com.example.tools.EngineeringCalculators
import com.example.tools.ResistorColorBand
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProjectPersistenceRobolectricTest {

    private lateinit var db: SalmanDatabase
    private lateinit var repo: ProjectRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SalmanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = ProjectRepository(db.projectDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `app name matches GateX Labs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("GateX Labs", context.getString(R.string.app_name))
    }

    @Test
    fun `project serializer round trips all pages components wires and junctions`() {
        val original = CircuitTemplates.byId("full_adder")!!.toNewProject(123456L)
        val json = ProjectSerializer.toJson(original)
        val restored = ProjectSerializer.fromJson(json)
        assertNotNull(restored)
        assertEquals(original.name, restored!!.name)
        assertEquals(original.pages.size, restored.pages.size)
        assertEquals(original.activePage().components.size, restored.activePage().components.size)
        assertEquals(original.activePage().wires.size, restored.activePage().wires.size)
        assertEquals(original.activePage().junctions.size, restored.activePage().junctions.size)
    }

    @Test
    fun `repository saves renames duplicates deletes and recovers autosave drafts`() = runBlocking {
        val proj = CircuitTemplates.byId("voltage_divider")!!.toNewProject(1000L)
        repo.saveProject(proj)
        var list = repo.savedProjects.first()
        assertEquals(1, list.size)

        val renamed = repo.renameProject(proj, "My Divider")
        assertEquals("My Divider", renamed.name)

        val copy = repo.duplicateProject(renamed)
        list = repo.savedProjects.first()
        assertEquals(2, list.size)

        repo.saveAutosaveDraft(copy.copy(name = "Draft Unsaved"))
        val recovered = repo.getRecoverableDraft()
        assertNotNull(recovered)
        assertEquals("Draft Unsaved", recovered!!.name)

        repo.clearAutosaveDraft()
        assertNull(repo.getRecoverableDraft())

        repo.deleteProject(copy.id)
        assertEquals(1, repo.savedProjects.first().size)
    }

    @Test
    fun `engineering calculators return accurate electronic values`() {
        val ohm = EngineeringCalculators.solveOhmsLaw(v = 12.0, i = null, r = 1000.0, p = null)!!
        assertEquals(0.012, ohm.currentA, 1e-6)
        assertEquals(0.144, ohm.powerW, 1e-6)

        val (ohms, tol) = EngineeringCalculators.decode4BandResistor(
            ResistorColorBand.RED,
            ResistorColorBand.RED,
            ResistorColorBand.BROWN,
            ResistorColorBand.GOLD
        )
        assertEquals(220.0, ohms, 0.01)
        assertEquals(5.0, tol, 0.01)

        val led = EngineeringCalculators.calculateLedResistor(5.0, 2.0, 15.0)!!
        assertTrue(led.exactOhms in 199.0..201.0)

        val base = EngineeringCalculators.convertNumberBase("255", 10)!!
        assertEquals("11111111", base.first)
        assertEquals("FF", base.third)
    }

    @Test
    fun `viewmodel connectWire branchAndConnectWire and rubber-band moveComponentTo work cleanly`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.WorkbenchViewModel(app)
        vm.createNewProject("Wire Test")
        vm.addComponent(com.example.model.ComponentType.DC_SOURCE, 4f, 4f)
        vm.addComponent(com.example.model.ComponentType.RESISTOR, 10f, 4f)
        vm.addComponent(com.example.model.ComponentType.LED, 16f, 8f)

        val comps = vm.activeProject.value.activePage().components
        val dc = comps[0]
        val res = comps[1]
        val led = comps[2]

        val pDcPos = com.example.model.WireEndpoint(dc.id, "pos")
        val pRes1 = com.example.model.WireEndpoint(res.id, "p1")
        val pLedA = com.example.model.WireEndpoint(led.id, "anode")

        assertTrue(vm.connectWire(pDcPos, pRes1))
        // Duplicate connection should be ignored
        assertTrue(!vm.connectWire(pDcPos, pRes1))
        assertEquals(1, vm.activeProject.value.activePage().wires.size)

        val firstWireId = vm.activeProject.value.activePage().wires.first().id
        vm.branchAndConnectWire(pLedA, firstWireId, 7f, 4f)
        val afterBranch = vm.activeProject.value.activePage()
        assertEquals(1, afterBranch.junctions.size)
        assertEquals(3, afterBranch.wires.size)

        // Moving component should dynamically re-route connected wires
        val oldWaypoints = afterBranch.wires.first { it.from == pLedA || it.to == pLedA }.waypoints
        vm.moveComponentTo(led.id, 18f, 12f)
        val newWaypoints = vm.activeProject.value.activePage().wires.first { it.from == pLedA || it.to == pLedA }.waypoints
        assertTrue(oldWaypoints != newWaypoints)
    }
}
