package com.zhehr.auralis.playback

import androidx.compose.runtime.Immutable
import androidx.media3.common.Player

data class QueueEntry(val id: Long, val albumId: Long, val title: String, val artist: String)

@Immutable
data class PlayerState(
    val connected: Boolean = false,
    val current: QueueEntry? = null,
    val album: String = "",
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val speed: Float = 1f,
    val queue: List<QueueEntry> = emptyList(),
    val queueIndex: Int = 0,
    val sleepRemainingMs: Long? = null,
    val sleepAtEndOfSong: Boolean = false,
)
