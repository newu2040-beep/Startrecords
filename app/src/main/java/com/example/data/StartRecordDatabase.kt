package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isCall: Boolean = false,
    val contactName: String? = null,
    val audioFormat: String = "M4A", // WAV, MP3, AAC, M4A
    val quality: String = "High", // High, Medium, Low
    val category: String = "General", // Meeting, Interview, Lecture, Personal, Call, Voice Note
    val notes: String? = null,
    val tags: String = "", // Comma-separated tags
    val noiseReduction: Boolean = false,
    val audioEnhancement: Boolean = false
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "recording_id", index = true) val recordingId: Int,
    val label: String,
    val timestampMs: Long
)

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings WHERE isArchived = 0 ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE isFavorite = 1 AND isArchived = 0 ORDER BY timestamp DESC")
    fun getFavoriteRecordings(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE isArchived = 1 ORDER BY timestamp DESC")
    fun getArchivedRecordings(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE category = :category AND isArchived = 0 ORDER BY timestamp DESC")
    fun getRecordingsByCategory(category: String): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE id = :id LIMIT 1")
    suspend fun getRecordingById(id: Int): Recording?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: Recording): Long

    @Update
    suspend fun updateRecording(recording: Recording)

    @Delete
    suspend fun deleteRecording(recording: Recording)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Int)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE recording_id = :recordingId ORDER BY timestampMs ASC")
    fun getBookmarksForRecording(recordingId: Int): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Int)

    @Query("DELETE FROM bookmarks WHERE recording_id = :recordingId")
    suspend fun deleteBookmarksForRecording(recordingId: Int)
}

@Database(entities = [Recording::class, Bookmark::class], version = 1, exportSchema = false)
abstract class StartRecordDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: StartRecordDatabase? = null

        fun getDatabase(context: Context): StartRecordDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StartRecordDatabase::class.java,
                    "startrecord_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class RecordingRepository(
    private val recordingDao: RecordingDao,
    private val bookmarkDao: BookmarkDao
) {
    val allRecordings: Flow<List<Recording>> = recordingDao.getAllRecordings()
    val favoriteRecordings: Flow<List<Recording>> = recordingDao.getFavoriteRecordings()
    val archivedRecordings: Flow<List<Recording>> = recordingDao.getArchivedRecordings()

    fun getRecordingsByCategory(category: String): Flow<List<Recording>> =
        recordingDao.getRecordingsByCategory(category)

    suspend fun getRecordingById(id: Int): Recording? =
        recordingDao.getRecordingById(id)

    suspend fun insertRecording(recording: Recording): Long =
        recordingDao.insertRecording(recording)

    suspend fun updateRecording(recording: Recording) =
        recordingDao.updateRecording(recording)

    suspend fun deleteRecording(recording: Recording) =
        recordingDao.deleteRecording(recording)

    suspend fun deleteRecordingById(id: Int) =
        recordingDao.deleteRecordingById(id)

    fun getBookmarksForRecording(recordingId: Int): Flow<List<Bookmark>> =
        bookmarkDao.getBookmarksForRecording(recordingId)

    suspend fun insertBookmark(bookmark: Bookmark) =
        bookmarkDao.insertBookmark(bookmark)

    suspend fun deleteBookmark(id: Int) =
        bookmarkDao.deleteBookmark(id)
}
