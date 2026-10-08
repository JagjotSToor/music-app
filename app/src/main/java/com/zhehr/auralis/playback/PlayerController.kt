package com.zhehr.auralis.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.zhehr.auralis.data.LibraryRepository
import com.zhehr.auralis.data.SettingsStore
import com.zhehr.auralis.data.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun SongEntity.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(Uri.parse("content://media/external/audio/albumart/$albumId"))
            .setExtras(Bundle().apply { putLong("albumId", albumId) })
            .build()
    )
    .build()

private fun MediaItem.toEntry() = QueueEntry(
    id = mediaId.toLongOrNull() ?: 0L,
    albumId = mediaMetadata.extras?.getLong("albumId") ?: 0L,
    title = mediaMetadata.title?.toString().orEmpty(),
    artist = mediaMetadata.artist?.toString().orEmpty(),
)

/**
 * UI-facing playback API. All Player calls happen on the main thread (MediaController requirement),
 * so this class owns a Main scope. Position is a separate flow so the 4 Hz ticker does not
 * invalidate everything that reads PlayerState.
 */
class PlayerController(
    private val context: Context,
    private val repo: LibraryRepository,
    private val settings: SettingsStore,
) {
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    val currentId: StateFlow<Long?> =
        state.map { it.current?.id }.distinctUntilChanged().stateIn(mainScope, SharingStarted.Eagerly, null)
    val queue: StateFlow<List<QueueEntry>> =
        state.map { it.queue }.distinctUntilChanged().stateIn(mainScope, SharingStarted.Eagerly, emptyList())
    val queueIndex: StateFlow<Int> =
        state.map { it.queueIndex }.distinctUntilChanged().stateIn(mainScope, SharingStarted.Eagerly, 0)

    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null
    private var tickJob: Job? = null
    private var sleepJob: Job? = null
    private var uiVisible = false

    fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener({
            val c = try { f.get() } catch (e: Exception) { future = null; return@addListener }
            controller = c
            c.addListener(listener)
            refresh(rebuildQueue = true)
            mainScope.launch { restoreSession(c) }
        }, ContextCompat.getMainExecutor(context))
    }

    fun onUiVisibility(visible: Boolean) {
        uiVisible = visible
        controller?.let { refresh(false) }
        updateTicker()
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refresh(events.contains(Player.EVENT_TIMELINE_CHANGED))
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED) && !player.isPlaying) saveSession()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val c = controller ?: return
            val id = mediaItem?.mediaId?.toLongOrNull() ?: return
            if (_state.value.sleepAtEndOfSong && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                c.pause()
                _state.update { it.copy(sleepAtEndOfSong = false) }
                saveSession()
                return
            }
            if (c.playWhenReady) mainScope.launch { repo.recordPlay(id) }
            saveSession()
        }
    }

    private fun refresh(rebuildQueue: Boolean) {
        val c = controller ?: return
        val item = c.currentMediaItem
        _state.update { s ->
            s.copy(
                connected = true,
                current = item?.toEntry(),
                album = item?.mediaMetadata?.albumTitle?.toString().orEmpty(),
                isPlaying = c.isPlaying,
                durationMs = c.duration.coerceAtLeast(0L),
                shuffle = c.shuffleModeEnabled,
                repeatMode = c.repeatMode,
                speed = c.playbackParameters.speed,
                queue = if (rebuildQueue) List(c.mediaItemCount) { c.getMediaItemAt(it).toEntry() } else s.queue,
                queueIndex = c.currentMediaItemIndex.coerceAtLeast(0),
            )
        }
        _position.value = c.currentPosition.coerceAtLeast(0L)
        updateTicker()
    }

    private fun updateTicker() {
        val playing = controller?.isPlaying == true
        if (playing && uiVisible) {
            if (tickJob?.isActive != true) {
                tickJob = mainScope.launch {
                    while (isActive) {
                        controller?.let { _position.value = it.currentPosition.coerceAtLeast(0L) }
                        delay(250)
                    }
                }
            }
        } else {
            tickJob?.cancel()
            tickJob = null
        }
    }

    // ---------------- commands ----------------

    /** Queues [songs] and starts at [startIndex]. Very large lists are windowed to keep IPC and memory small. */
    fun playSongs(songs: List<SongEntity>, startIndex: Int, shuffle: Boolean? = null) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        var list = songs
        var start = startIndex.coerceIn(0, songs.lastIndex)
        if (songs.size > MAX_QUEUE) {
            val from = (start - 200).coerceAtLeast(0)
            list = songs.subList(from, minOf(songs.size, from + MAX_QUEUE))
            start -= from
        }
        c.setMediaItems(list.map { it.toMediaItem() }, start, 0L)
        if (shuffle != null) c.shuffleModeEnabled = shuffle
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() { controller?.seekToNext() }
    fun previous() { controller?.seekToPrevious() }
    fun seekTo(ms: Long) { controller?.seekTo(ms); _position.value = ms }

    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
        mainScope.launch { settings.setSpeed(speed) }
    }

    fun playNext(song: SongEntity) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) playSongs(listOf(song), 0)
        else c.addMediaItem(c.currentMediaItemIndex + 1, song.toMediaItem())
    }

    fun addToQueue(song: SongEntity) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) playSongs(listOf(song), 0) else c.addMediaItem(song.toMediaItem())
    }

    fun playQueueIndex(index: Int) { controller?.let { it.seekTo(index, 0L); it.play() } }
    fun removeFromQueue(index: Int) { controller?.removeMediaItem(index) }
    fun moveInQueue(from: Int, to: Int) { controller?.moveMediaItem(from, to) }
    fun clearQueue() { controller?.clearMediaItems() }

    // ---------------- sleep timer ----------------

    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        sleepJob = null
        _state.update { it.copy(sleepAtEndOfSong = false, sleepRemainingMs = null) }
        if (minutes == null) return
        val endAt = SystemClock.elapsedRealtime() + minutes * 60_000L
        sleepJob = mainScope.launch {
            while (isActive) {
                val left = endAt - SystemClock.elapsedRealtime()
                if (left <= 0) {
                    controller?.pause()
                    _state.update { it.copy(sleepRemainingMs = null) }
                    break
                }
                _state.update { it.copy(sleepRemainingMs = left) }
                delay(1000)
            }
        }
    }

    fun sleepAfterCurrentSong() {
        sleepJob?.cancel()
        sleepJob = null
        _state.update { it.copy(sleepAtEndOfSong = true, sleepRemainingMs = null) }
    }

    // ---------------- persistence ----------------

    private fun saveSession() {
        val c = controller ?: return
        val n = c.mediaItemCount
        if (n == 0) return
        val ids = List(n) { c.getMediaItemAt(it).mediaId.toLongOrNull() ?: 0L }
        val index = c.currentMediaItemIndex
        val pos = c.currentPosition
        mainScope.launch { settings.saveSession(ids, index, pos) }
    }

    private suspend fun restoreSession(c: MediaController) {
        c.setPlaybackSpeed(settings.getSpeed())
        if (c.mediaItemCount > 0) return // service is already playing; do not override it
        val saved = settings.loadSession() ?: return
        val songs = repo.songsByIdsOrdered(saved.ids)
        if (songs.isEmpty() || c.mediaItemCount > 0) return
        val wantedId = saved.ids.getOrNull(saved.index)
        val idx = songs.indexOfFirst { it.id == wantedId }.let { if (it < 0) 0 else it }
        val pos = if (songs[idx].id == wantedId) saved.positionMs else 0L
        c.setMediaItems(songs.map { it.toMediaItem() }, idx, pos)
        c.prepare() // paused: resume is one tap away
    }

    private companion object {
        const val MAX_QUEUE = 2000
    }
}
