package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.CircuitProject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long,
    val projectJson: String,
    val isAutosaveDraft: Boolean = false
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE isAutosaveDraft = 0 ORDER BY updatedAt DESC")
    fun observeSavedProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isAutosaveDraft = 1 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestAutosaveDraft(): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProjectEntity?

    @Query("SELECT COUNT(*) FROM projects WHERE isAutosaveDraft = 0")
    suspend fun countSaved(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM projects WHERE isAutosaveDraft = 1")
    suspend fun clearAutosaveDrafts()
}

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
abstract class SalmanDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: SalmanDatabase? = null

        fun getInstance(context: Context): SalmanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SalmanDatabase::class.java,
                    "salman_digital.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class ProjectRepository(private val dao: ProjectDao) {

    val savedProjects: Flow<List<CircuitProject>> = dao.observeSavedProjects().map { list ->
        list.mapNotNull { ProjectSerializer.fromJson(it.projectJson) }
    }

    suspend fun ensureStarterProjectsSeeded() {
        if (dao.countSaved() == 0) {
            val now = System.currentTimeMillis()
            val starters = listOf(
                CircuitTemplates.byId("full_adder")!!.toNewProject(now - 300_000),
                CircuitTemplates.byId("led_resistor")!!.toNewProject(now - 200_000),
                CircuitTemplates.byId("rc_charging")!!.toNewProject(now - 100_000),
            )
            starters.forEach { saveProject(it) }
        }
    }

    suspend fun saveProject(project: CircuitProject) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        dao.upsert(
            ProjectEntity(
                id = updated.id,
                name = updated.name,
                description = updated.description,
                createdAt = updated.createdAt,
                updatedAt = updated.updatedAt,
                projectJson = ProjectSerializer.toJson(updated),
                isAutosaveDraft = false
            )
        )
    }

    suspend fun saveAutosaveDraft(project: CircuitProject) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        dao.upsert(
            ProjectEntity(
                id = "autosave_draft",
                name = updated.name,
                description = updated.description,
                createdAt = updated.createdAt,
                updatedAt = updated.updatedAt,
                projectJson = ProjectSerializer.toJson(updated),
                isAutosaveDraft = true
            )
        )
    }

    suspend fun getRecoverableDraft(): CircuitProject? {
        val entity = dao.getLatestAutosaveDraft() ?: return null
        return ProjectSerializer.fromJson(entity.projectJson)
    }

    suspend fun clearAutosaveDraft() {
        dao.clearAutosaveDrafts()
    }

    suspend fun deleteProject(projectId: String) {
        dao.deleteById(projectId)
    }

    suspend fun duplicateProject(project: CircuitProject): CircuitProject {
        val now = System.currentTimeMillis()
        val copy = project.copy(
            id = "proj_${now}",
            name = "${project.name} (Copy)",
            createdAt = now,
            updatedAt = now
        )
        saveProject(copy)
        return copy
    }

    suspend fun renameProject(project: CircuitProject, newName: String): CircuitProject {
        val renamed = project.copy(name = newName.trim().ifEmpty { project.name }, updatedAt = System.currentTimeMillis())
        saveProject(renamed)
        return renamed
    }
}
