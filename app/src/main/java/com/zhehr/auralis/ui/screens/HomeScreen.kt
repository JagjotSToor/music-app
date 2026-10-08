package com.zhehr.auralis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.zhehr.auralis.LocalContainer
import com.zhehr.auralis.data.SongEntity
import com.zhehr.auralis.theme.LocalAuralisPalette
import com.zhehr.auralis.ui.components.ArtworkImage
import com.zhehr.auralis.ui.components.EmptyState
import java.time.LocalTime

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

@Composable
fun HomeScreen(nav: NavHostController) {
    val c = LocalContainer.current
    val palette = LocalAuralisPalette.current
    val hello = remember { greeting() }
    val total by c.repo.count.collectAsStateWithLifecycle(initialValue = 0)
    val scan by c.scanner.state.collectAsStateWithLifecycle()
    val recentlyPlayed by c.repo.recentlyPlayed.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentlyAdded by c.repo.recentlyAdded.collectAsStateWithLifecycle(initialValue = emptyList())
    val mostPlayed by c.repo.mostPlayed.collectAsStateWithLifecycle(initialValue = emptyList())
    val favorites by c.repo.favorites.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)) {
            // Brand hierarchy: AURALIS is the product; ZHEHR stays small and secondary.
            Text("AURALIS", style = MaterialTheme.typography.titleLarge, color = palette.text)
            Text("by ZHEHR", style = MaterialTheme.typography.labelSmall, color = palette.mutedText)
            Text(hello, style = MaterialTheme.typography.headlineMedium, color = palette.text, modifier = Modifier.padding(top = 20.dp))
        }
        if (total == 0) {
            EmptyState(
                title = if (scan.running) "Scanning your music…" else "No music found yet",
                body = if (scan.running) "This only takes a moment on first launch."
                else "Copy some audio files to this phone, then open Settings and tap Rescan.",
                modifier = Modifier.padding(top = 48.dp),
            )
        } else {
            SongShelf("Recently played", recentlyPlayed)
            SongShelf("Recently added", recentlyAdded)
            SongShelf("Most played", mostPlayed)
            SongShelf("Favorites", favorites, onSeeAll = { nav.navigate("detail/favorites/0") })
        }
    }
}

@Composable
private fun SongShelf(title: String, songs: List<SongEntity>, onSeeAll: (() -> Unit)? = null) {
    if (songs.isEmpty()) return
    val player = LocalContainer.current.player
    val palette = LocalAuralisPalette.current
    Column(Modifier.padding(bottom = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = palette.text, modifier = Modifier.weight(1f))
            if (onSeeAll != null) TextButton(onClick = onSeeAll) { Text("See all") }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            itemsIndexed(songs, key = { _, s -> s.id }) { i, s ->
                Column(Modifier.width(132.dp).clickable { player.playSongs(songs, i) }) {
                    ArtworkImage(s.id, s.albumId, Modifier.size(132.dp), px = 320, shape = RoundedCornerShape(16.dp))
                    Text(s.title, style = MaterialTheme.typography.labelLarge, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                    Text(s.artist, style = MaterialTheme.typography.bodyMedium, color = palette.mutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
