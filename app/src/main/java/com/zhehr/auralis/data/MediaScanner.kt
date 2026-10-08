package com.zhehr.auralis.data

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media as M
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

data class ScanState(val running: Boolean = false, val message: String = "")

/**
 * Incremental scanner. MediaStore already holds the metadata, so we read it in one query and only
 * write rows whose (id, dateModified, size) differ from what the database has. Rows for files that
 * no longer exist are removed.
 */
class MediaScanner(private val context: Context, private val db: AuralisDatabase) {
    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()
    private val mutex = Mutex()

    @Suppress("DEPRECATION")
    private val folderColumn: String =
        if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA

    suspend fun scan(): Unit = withContext(Dispatchers.IO) {
        if (!mutex.tryLock()) return@withContext
        try {
            _state.value = ScanState(true, "Scanning your music…")
            val dao = db.songDao()
            val existing = dao.syncInfo().associateBy { it.id }
            val seen = HashSet<Long>()
            val batch = ArrayList<SongEntity>()
            var added = 0
            var changed = 0

            val projection = arrayOf(
                M._ID, M.TITLE, M.ARTIST, M.ALBUM, M.ALBUM_ID, M.YEAR, M.TRACK,
                M.DURATION, M.SIZE, M.DATE_ADDED, M.DATE_MODIFIED, folderColumn,
            )
            val selection = "${M.IS_MUSIC} != 0 AND ${M.DURATION} >= 10000"

            context.contentResolver.query(M.EXTERNAL_CONTENT_URI, projection, selection, null, null)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(M._ID)
                val titleCol = c.getColumnIndexOrThrow(M.TITLE)
                val artistCol = c.getColumnIndexOrThrow(M.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(M.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(M.ALBUM_ID)
                val yearCol = c.getColumnIndexOrThrow(M.YEAR)
                val trackCol = c.getColumnIndexOrThrow(M.TRACK)
                val durCol = c.getColumnIndexOrThrow(M.DURATION)
                val sizeCol = c.getColumnIndexOrThrow(M.SIZE)
                val addedCol = c.getColumnIndexOrThrow(M.DATE_ADDED)
                val modCol = c.getColumnIndexOrThrow(M.DATE_MODIFIED)
                val folderCol = c.getColumnIndexOrThrow(folderColumn)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    seen.add(id)
                    val mod = c.getLong(modCol)
                    val size = c.getLong(sizeCol)
                    val old = existing[id]
                    if (old != null && old.dateModified == mod && old.sizeBytes == size) continue

                    val rawFolder = c.getString(folderCol).orEmpty()
                    val folder = if (Build.VERSION.SDK_INT >= 29) rawFolder.trimEnd('/') else rawFolder.substringBeforeLast('/', "")

                    batch.add(
                        SongEntity(
                            id = id,
                            title = c.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Unknown title",
                            artist = clean(c.getString(artistCol), "Unknown artist"),
                            album = clean(c.getString(albumCol), "Unknown album"),
                            albumId = c.getLong(albumIdCol),
                            year = c.getInt(yearCol),
                            trackNo = c.getInt(trackCol) % 1000,
                            durationMs = c.getLong(durCol),
                            sizeBytes = size,
                            dateAdded = c.getLong(addedCol),
                            dateModified = mod,
                            folder = folder.ifBlank { "Unknown folder" },
                        )
                    )
                    if (old == null) added++ else changed++
                    if (batch.size >= 300) {
                        dao.upsertAll(batch.toList())
                        batch.clear()
                    }
                }
            }
            if (batch.isNotEmpty()) dao.upsertAll(batch.toList())

            val gone = existing.keys.filter { it !in seen }
            gone.chunked(500).forEach { dao.deleteByIds(it) }

            _state.value = ScanState(false, "${seen.size} songs · $added new, $changed updated, ${gone.size} removed")
        } catch (e: SecurityException) {
            _state.value = ScanState(false, "Music permission is missing.")
        } catch (e: Exception) {
            _state.value = ScanState(false, "Scan failed: ${e.message}")
        } finally {
            mutex.unlock()
        }
    }

    private fun clean(value: String?, fallback: String): String =
        if (value.isNullOrBlank() || value == "<unknown>") fallback else value
}
