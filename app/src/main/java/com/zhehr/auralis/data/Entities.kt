package com.zhehr.auralis.data

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Primary key is the stable MediaStore id, so rescans update rows instead of duplicating them. */
@Entity(tableName = "song", indices = [Index("albumId"), Index("artist"), Index("folder"), Index("dateAdded")])
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val year: Int,
    val trackNo: Int,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val folder: String,
) {
    val uri: Uri get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
}

@Entity(tableName = "favorite")
data class FavoriteEntity(@PrimaryKey val songId: Long, val addedAt: Long)

@Entity(tableName = "playlist")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

/** Composite key means a song can only be in a playlist once (duplicate detection). */
@Entity(tableName = "playlist_song", primaryKeys = ["playlistId", "songId"], indices = [Index("songId")])
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: Long,
    val position: Int,
    val addedAt: Long,
)

@Entity(tableName = "play_history", indices = [Index("songId"), Index("playedAt")])
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val playedAt: Long,
)
