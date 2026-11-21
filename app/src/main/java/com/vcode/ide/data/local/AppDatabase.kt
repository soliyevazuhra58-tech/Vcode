package com.vcode.ide.data.local

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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val rootPath: String,
    val templateId: String,
    val lastOpenedAt: Long,
    val linkedTreeUri: String? = null
)

@Entity(tableName = "recent_tabs")
data class RecentTabEntity(
    @PrimaryKey val filePath: String,
    val projectId: String,
    val lastOpenedAt: Long,
    val cursorLine: Int,
    val cursorColumn: Int
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastOpenedAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY lastOpenedAt DESC LIMIT 1")
    suspend fun getMostRecentProject(): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String): Int

    @Query("SELECT * FROM recent_tabs WHERE projectId = :projectId ORDER BY lastOpenedAt ASC")
    suspend fun getRecentTabsForProject(projectId: String): List<RecentTabEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentTab(tab: RecentTabEntity)

    @Query("DELETE FROM recent_tabs WHERE projectId = :projectId")
    suspend fun clearTabsForProject(projectId: String): Int

    @Query("DELETE FROM recent_tabs WHERE filePath = :filePath")
    suspend fun deleteTab(filePath: String): Int
}

@Database(
    entities = [ProjectEntity::class, RecentTabEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vcode_ide.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
