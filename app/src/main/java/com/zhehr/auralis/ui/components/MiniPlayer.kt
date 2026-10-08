package com.zhehr.auralis.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhehr.auralis.LocalContainer
import com.zhehr.auralis.R
import com.zhehr.auralis.theme.LocalAuralisPalette

@Composable
fun MiniPlayer(onOpen: () -> Unit) {
    val player = LocalContainer.current.player
    val palette = LocalAuralisPalette.current
    val st by player.state.collectAsStateWithLifecycle()
    val pos by player.position.collectAsStateWithLifecycle()
    val cur = st.current ?: return
    val frac = if (st.durationMs > 0) (pos.toFloat() / st.durationMs).coerceIn(0f, 1f) else 0f

    GlassSurface(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(64.dp)
            .clickable(onClick = onOpen)
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -40f) onOpen() },
                    onVerticalDrag = { _, dy -> total += dy },
                )
            },
    ) {
        Row(Modifier.fillMaxSize().padding(start = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ArtworkImage(cur.id, cur.albumId, Modifier.size(44.dp), px = 144)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(cur.title, style = MaterialTheme.typography.titleMedium, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(cur.artist, style = MaterialTheme.typography.bodyMedium, color = palette.mutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { player.togglePlay() }) {
                Icon(
                    painterResource(if (st.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = if (st.isPlaying) "Pause" else "Play",
                    tint = palette.text,
                )
            }
            IconButton(onClick = { player.next() }) {
                Icon(painterResource(R.drawable.ic_next), contentDescription = "Next song", tint = palette.text)
            }
        }
        LinearProgressIndicator(
            progress = { frac },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp),
            color = palette.accent,
            trackColor = Color.Transparent,
        )
    }
}
