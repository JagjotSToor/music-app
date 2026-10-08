package com.zhehr.auralis.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LibraryRepository(db: AuralisDatabase) {
    private val songs = db.songDao()
    private val fav = db.favoriteDao()
    private val pl = db.playlistDao()
    private val hist = db.historyDao()

    val allSongs: Flow<List<SongEntity>> = songs.observeAll()
    val count: Flow<Int> = songs.observeCount()
    val albums: Flow<List<AlbumRow>> = songs.observeAlbums()
    val artists: Flow<List<ArtistRow>> = songs.observeArtists()
    val folders: Flow<List<FolderRow>> = songs.observeFolders()
    val recentlyAdded: Flow<List<SongEntity>> = songs.observeRecentlyAdded(20)
    val recentlyPlayed: Flow<List<SongEntity>> = songs.observeRecentlyPlayed(20)
    val mostPlayed: Flow<List<SongEntity>> = songs.observeMostPlayed(20)
    val favorites: Flow<List<SongEntity>> = songs.observeFavorites()
    val favoriteIds: Flow<Set<Long>> = fav.observeIds().map { it.toSet() }
    val playlists: Flow<List<PlaylistRow>> = pl.observeAll()

    fun byAlbum(id: Long) = songs.observeByAlbum(id)
    fun byArtist(name: String) = songs.observeByArtist(name)
    fun byFolder(path: String) = songs.observeByFolder(path)
    fun byPlaylist(id: Long) = songs.observeByPlaylist(id)

    suspend fun search(q: String): List<SongEntity> = songs.search(q)

    suspend fun setFavorite(id: Long, on: Boolean) {
        if (on) fav.add(FavoriteEntity(id, System.currentTimeMillis())) else fav.remove(id)
    }

    suspend fun createPlaylist(name: String): Long =
        pl.insert(PlaylistEntity(name = name, createdAt = System.currentTimeMillis()))

    /** Returns false if the song was already in the playlist. */
    suspend fun addToPlaylist(playlistId: Long, songId: Long): Boolean {
        val pos = pl.nextPosition(playlistId)
        return pl.addSong(PlaylistSongEntity(playlistId, songId, pos, System.currentTimeMillis())) != -1L
    }

    suspend fun removeFromPlaylist(playlistId: Long, songId: Long) = pl.removeSong(playlistId, songId)

    suspend fun deletePlaylist(id: Long) {
        pl.clear(id)
        pl.delete(id)
    }

    suspend fun playlistName(id: Long): String? = pl.name(id)

    suspend fun createPlaylistWithSongs(name: String, songIds: List<Long>): Long {
        val id = createPlaylist(name)
        songIds.distinct().forEach { addToPlaylist(id, it) }
        return id
    }

    suspend fun recordPlay(songId: Long) = hist.add(PlayHistoryEntity(songId = songId, playedAt = System.currentTimeMillis()))

    /** Looks songs up in chunks (SQLite variable limit) and returns them in the order of [ids]. */
    suspend fun songsByIdsOrdered(ids: List<Long>): List<SongEntity> {
        val map = ids.chunked(500).flatMap { songs.byIds(it) }.associateBy { it.id }
        return ids.mapNotNull { map[it] }
    }
}
