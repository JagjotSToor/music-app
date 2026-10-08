package com.zhehr.auralis.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.zhehr.auralis.LocalContainer
import com.zhehr.auralis.R
import com.zhehr.auralis.theme.AuralisPalettes
import com.zhehr.auralis.theme.Contrast
import com.zhehr.auralis.ui.components.ArtColors
import com.zhehr.auralis.ui.components.ArtworkImage
import com.zhehr.auralis.ui.components.TiltCard
import com.zhehr.auralis.ui.components.formatTime
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

private val Ink = Color(0xFF0B0D12)

/** Always dark and immersive regardless of the app theme, so white text over the scrim is always readable. */
@Composable
fun NowPlayingScreen(onClose: () -> Unit, reduceMotion: Boolean) {
    val c = LocalContainer.current
    val player = c.player
    val scope = rememberCoroutineScope()
    val st by player.state.collectAsStateWithLifecycle()
    val favIds by c.repo.favoriteIds.collectAsStateWithLifecycle(initialValue = emptySet())
    var showQueue by remember { mutableStateOf(false) }
    BackHandler(enabled = showQueue) { showQueue = false }

    val cur = st.current
    val art by produceState<ArtColors?>(null, cur?.id, cur?.albumId) {
        value = if (cur != null) c.artwork.colors(cur.id, cur.albumId) else null
    }
    val blurBmp by produceState<Bitmap?>(null, cur?.id, cur?.albumId) {
        value = if (cur != null) c.artwork.blurred(cur.id, cur.albumId) else null
    }
    val fadeMs = if (reduceMotion) 0 else 500
    val accent by animateColorAsState(art?.accent ?: AuralisPalettes.Dark.accent, tween(fadeMs), label = "accent")
    val tint by animateColorAsState(art?.tint ?: Ink, tween(fadeMs), label = "tint")
    val playScale by animateFloatAsState(if (st.isPlaying || reduceMotion) 1f else 0.93f, spring(), label = "scale")

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(Modifier.fillMaxSize().background(Ink).pointerInput(Unit) { detectTapGestures { } }) {
            blurBmp?.let { b ->
                Image(
                    remember(b) { b.asImageBitmap() }, null,
                    Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.Medium, alpha = 0.9f,
                )
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.35f), Ink.copy(alpha = 0.92f)))))

            if (cur != null) {
                val isFav = cur.id in favIds
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close player") }
                        Text(
                            "NOW PLAYING", style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { showQueue = true }) { Icon(painterResource(R.drawable.ic_queue), contentDescription = "Open queue") }
                    }

                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val side = (minOf(maxWidth, maxHeight) - 16.dp).coerceAtLeast(120.dp)
                        TiltCard(
                            enabled = !reduceMotion,
                            modifier = Modifier
                                .size(side)
                                .graphicsLayer { scaleX = playScale; scaleY = playScale }
                                .shadow(24.dp, RoundedCornerShape(28.dp), ambientColor = accent, spotColor = accent),
                        ) {
                            Crossfade(targetState = cur, animationSpec = tween(fadeMs), label = "art") { e ->
                                ArtworkImage(e.id, e.albumId, Modifier.fillMaxSize(), px = 720, shape = RoundedCornerShape(28.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(cur.title, style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(cur.artist, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (st.album.isNotBlank()) {
                                Text(st.album, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        IconButton(onClick = { scope.launch { c.repo.setFavorite(cur.id, !isFav) } }) {
                            Icon(
                                if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFav) "Remove from favorites" else "Add to favorites",
                                tint = if (isFav) accent else Color.White,
                            )
                        }
                    }

                    SeekBar(st.durationMs, accent, player.position, onSeek = { player.seekTo(it) })

                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { player.toggleShuffle() }) {
                            Icon(painterResource(R.drawable.ic_shuffle), contentDescription = if (st.shuffle) "Shuffle on" else "Shuffle off", tint = if (st.shuffle) accent else Color.White.copy(alpha = 0.7f))
                        }
                        IconButton(onClick = { player.previous() }, modifier = Modifier.size(56.dp)) {
                            Icon(painterResource(R.drawable.ic_prev), contentDescription = "Previous song", modifier = Modifier.size(32.dp))
                        }
                        Box(
                            Modifier.size(72.dp).clip(CircleShape).background(accent).clickable { player.togglePlay() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(if (st.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                                contentDescription = if (st.isPlaying) "Pause" else "Play",
                                tint = Contrast.readableOn(accent, Ink),
                                modifier = Modifier.size(36.dp),
                            )
                        }
                        IconButton(onClick = { player.next() }, modifier = Modifier.size(56.dp)) {
                            Icon(painterResource(R.drawable.ic_next), contentDescription = "Next song", modifier = Modifier.size(32.dp))
                        }
                        IconButton(onClick = { player.cycleRepeat() }) {
                            val on = st.repeatMode != Player.REPEAT_MODE_OFF
                            Icon(
                                painterResource(if (st.repeatMode == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat),
                                contentDescription = when (st.repeatMode) {
                                    Player.REPEAT_MODE_ONE -> "Repeat one"
                                    Player.REPEAT_MODE_ALL -> "Repeat all"
                                    else -> "Repeat off"
                                },
                                tint = if (on) accent else Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }

                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        SpeedButton(st.speed, accent) { player.setSpeed(it) }
                        Spacer(Modifier.width(16.dp))
                        SleepButton(
                            active = st.sleepRemainingMs != null || st.sleepAtEndOfSong,
                            accent = accent,
                            onMinutes = { player.setSleepTimer(it) },
                            onEndOfSong = { player.sleepAfterCurrentSong() },
                        )
                        val left = st.sleepRemainingMs
                        if (left != null) Text(formatTime(left), style = MaterialTheme.typography.labelSmall, color = accent)
                        else if (st.sleepAtEndOfSong) Text("after this song", style = MaterialTheme.typography.labelSmall, color = accent)
                    }
                }
            }

            AnimatedVisibility(
                visible = showQueue,
                enter = slideInVertically(tween(if (reduceMotion) 0 else 300)) { it } + fadeIn(tween(if (reduceMotion) 0 else 300)),
                exit = slideOutVertically(tween(if (reduceMotion) 0 else 250)) { it } + fadeOut(tween(if (reduceMotion) 0 else 250)),
                modifier = Modifier.fillMaxSize(),
            ) {
                QueuePanel(accent = accent, onClose = { showQueue = false })
            }
        }
    }
}

@Composable
private fun SeekBar(durationMs: Long, accent: Color, positionFlow: StateFlow<Long>, onSeek: (Long) -> Unit) {
    val pos by positionFlow.collectAsStateWithLifecycle()
    var drag by remember { mutableStateOf<Float?>(null) }
    val dur = durationMs.coerceAtLeast(1L).toFloat()
    val shown = (drag ?: pos.toFloat()).coerceIn(0f, dur)
    Column {
        Slider(
            value = shown,
            onValueChange = { drag = it },
            onValueChangeFinished = { drag?.let { onSeek(it.toLong()) }; drag = null },
            valueRange = 0f..dur,
            colors = SliderDefaults.colors(
                thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = Color.White.copy(alpha = 0.2f),
            ),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(shown.toLong()), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            Text("-" + formatTime(dur.toLong() - shown.toLong()), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

private fun fmtSpeed(s: Float): String =
    (if (s % 1f == 0f) s.toInt().toString() else String.format(Locale.US, "%.2f", s).trimEnd('0')) + "x"

@Composable
private fun SpeedButton(speed: Float, accent: Color, onPick: (Float) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text("Speed ${fmtSpeed(speed)}", color = if (speed != 1f) accent else Color.White)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { s ->
                DropdownMenuItem(text = { Text(fmtSpeed(s)) }, onClick = { open = false; onPick(s) })
            }
        }
    }
}

@Composable
private fun SleepButton(active: Boolean, accent: Color, onMinutes: (Int?) -> Unit, onEndOfSong: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                painterResource(R.drawable.ic_timer),
                contentDescription = "Sleep timer",
                tint = if (active) accent else Color.White.copy(alpha = 0.7f),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(5, 10, 15, 30, 45, 60).forEach { m ->
                DropdownMenuItem(text = { Text("$m minutes") }, onClick = { open = false; onMinutes(m) })
            }
            DropdownMenuItem(text = { Text("End of this song") }, onClick = { open = false; onEndOfSong() })
            if (active) DropdownMenuItem(text = { Text("Turn off") }, onClick = { open = false; onMinutes(null) })
        }
    }
}

@Composable
private fun QueuePanel(accent: Color, onClose: () -> Unit) {
    val c = LocalContainer.current
    val player = c.player
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val queue by player.queue.collectAsStateWithLifecycle()
    val index by player.queueIndex.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var saveDialog by remember { mutableStateOf(false) }
    var clearDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { if (index in queue.indices) listState.scrollToItem(index) }

    Column(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.97f)).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Up next · ${queue.size}", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
            TextButton(onClick = { saveDialog = true }, enabled = queue.isNotEmpty()) { Text("Save") }
            TextButton(onClick = { clearDialog = true }, enabled = queue.isNotEmpty()) { Text("Clear") }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close queue") }
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(queue, key = { i, e -> "$i-${e.id}" }) { i, e ->
                Row(
                    Modifier.fillMaxWidth().clickable { player.playQueueIndex(i) }.padding(start = 20.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtworkImage(e.id, e.albumId, Modifier.size(40.dp), px = 120)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.title, style = MaterialTheme.typography.titleMedium, color = if (i == index) accent else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(e.artist, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(enabled = i > 0, onClick = { player.moveInQueue(i, i - 1) }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move ${e.title} up")
                    }
                    IconButton(enabled = i < queue.lastIndex, onClick = { player.moveInQueue(i, i + 1) }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move ${e.title} down")
                    }
                    IconButton(onClick = { player.removeFromQueue(i) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove ${e.title} from queue")
                    }
                }
            }
        }
    }

    if (saveDialog) {
        AlertDialog(
            onDismissRequest = { saveDialog = false },
            title = { Text("Save queue as playlist") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Playlist name") }) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    val ids = queue.map { it.id }
                    val n = name.trim()
                    scope.launch {
                        c.repo.createPlaylistWithSongs(n, ids)
                        Toast.makeText(ctx, "Saved \"$n\"", Toast.LENGTH_SHORT).show()
                    }
                    name = ""
                    saveDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { saveDialog = false }) { Text("Cancel") } },
        )
    }
    if (clearDialog) {
        AlertDialog(
            onDismissRequest = { clearDialog = false },
            title = { Text("Clear the queue?") },
            text = { Text("Playback will stop. Your songs are not deleted.") },
            confirmButton = { TextButton(onClick = { clearDialog = false; player.clearQueue(); onClose() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { clearDialog = false }) { Text("Cancel") } },
        )
    }
}
