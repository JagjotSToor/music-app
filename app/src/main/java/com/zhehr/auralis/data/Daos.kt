package com.zhehr.auralis.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class SyncInfo(val id: Long, val dateModified: Long, val sizeBytes: Long)
data class AlbumRow(val songId: Long, val albumId: Long, val album: String, val artist: String, val songCount: Int)
data class ArtistRow(val songId: Long, val albumId: Long, val artist: String, val songCount: Int, val albumCount: Int)
data class FolderRow(val folder: String, val songCount: Int)
data class PlaylistRow(val id: Long, val name: String, val songCount: Int)

@Dao
interface SongDao {
    @Query("SELECT id, dateModified, sizeBytes FROM song")
    suspend fun syncInfo(): List<SyncInfo>

    @Upsert
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("DELETE FROM song WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM song ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM song")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM song ORDER BY dateAdded DESC LIMIT :n")
    fun observeRecentlyAdded(n: Int): Flow<List<SongEntity>>

    @Query("SELECT s.* FROM song s INNER JOIN favorite f ON f.songId = s.id ORDER BY f.addedAt DESC")
    fun observeFavorites(): Flow<List<SongEntity>>

    @Query(
        "SELECT s.* FROM song s INNER JOIN " +
            "(SELECT songId, MAX(playedAt) AS m FROM play_history GROUP BY songId) h ON h.songId = s.id " +
            "ORDER BY h.m DESC LIMIT :n"
    )
    fun observeRecentlyPlayed(n: Int): Flow<List<SongEntity>>

    @Query(
        "SELECT s.* FROM song s INNER JOIN " +
            "(SELECT songId, COUNT(*) AS c, MAX(playedAt) AS m FROM play_history GROUP BY songId) h ON h.songId = s.id " +
            "ORDER BY h.c DESC, h.m DESC LIMIT :n"
    )
    fun observeMostPlayed(n: Int): Flow<List<SongEntity>>

    @Query(
        "SELECT MIN(id) AS songId, albumId, album, MIN(artist) AS artist, COUNT(*) AS songCount " +
            "FROM song GROUP BY albumId ORDER BY album COLLATE NOCASE"
    )
    fun observeAlbums(): Flow<List<AlbumRow>>

    @Query(
        "SELECT MIN(id) AS songId, MIN(albumId) AS albumId, artist, COUNT(*) AS songCount, " +
            "COUNT(DISTINCT albumId) AS albumCount FROM song GROUP BY artist ORDER BY artist COLLATE NOCASE"
    )
    fun observeArtists(): Flow<List<ArtistRow>>

    @Query("SELECT folder, COUNT(*) AS songCount FROM song GROUP BY folder ORDER BY folder COLLATE NOCASE")
    fun observeFolders(): Flow<List<FolderRow>>

    @Query("SELECT * FROM song WHERE albumId = :albumId ORDER BY trackNo, title COLLATE NOCASE")
    fun observeByAlbum(albumId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE artist = :artist ORDER BY album COLLATE NOCASE, trackNo, title COLLATE NOCASE")
    fun observeByArtist(artist: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE folder = :folder ORDER BY title COLLATE NOCASE")
    fun observeByFolder(folder: String): Flow<List<SongEntity>>

    @Query(
        "SELECT s.* FROM song s INNER JOIN playlist_song p ON p.songId = s.id " +
            "WHERE p.playlistId = :playlistId ORDER BY p.position"
    )
    fun observeByPlaylist(playlistId: Long): Flow<List<SongEntity>>

    @Query(
        "SELECT * FROM song WHERE title LIKE '%' || :q || '%' OR artist LIKE '%' || :q || '%' " +
            "OR album LIKE '%' || :q || '%' ORDER BY title COLLATE NOCASE LIMIT 200"
    )
    suspend fun search(q: String): List<SongEntity>

    @Query("SELECT * FROM song WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<SongEntity>
}

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE songId = :songId")
    suspend fun remove(songId: Long)

    @Query("SELECT songId FROM favorite")
    fun observeIds(): Flow<List<Long>>
}

@Dao
interface PlaylistDao {
    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSong(entry: PlaylistSongEntity): Long

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_song WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Query("DELETE FROM playlist_song WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSong(playlistId: Long, songId: Long)

    @Query("DELETE FROM playlist_song WHERE playlistId = :playlistId")
    suspend fun clear(playlistId: Long)

    @Query("DELETE FROM playlist WHERE id = :playlistId")
    suspend fun delete(playlistId: Long)

    @Query("SELECT name FROM playlist WHERE id = :playlistId")
    suspend fun name(playlistId: Long): String?

    @Query(
        "SELECT p.id AS id, p.name AS name, COUNT(ps.songId) AS songCount FROM playlist p " +
            "LEFT JOIN playlist_song ps ON ps.playlistId = p.id GROUP BY p.id ORDER BY p.createdAt DESC"
    )
    fun observeAll(): Flow<List<PlaylistRow>>
}

@Dao
interface HistoryDao {
    @Insert
    suspend fun add(entry: PlayHistoryEntity)
}
