package com.zhehr.auralis.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
import com.zhehr.auralis.ui.components.EmptyState
import com.zhehr.auralis.ui.components.SongRow
import kotlinx.coroutines.flow.Flow

@Composable
fun DetailScreen(kind: String, key: String, nav: NavHostController) {
    val c = LocalContainer.current
    val palette = LocalAuralisPalette.current
    val flow: Flow<List<SongEntity>> = remember(kind, key) {
        when (kind) {
            "album" -> c.repo.byAlbum(key.toLongOrNull() ?: 0L)
            "artist" -> c.repo.byArtist(key)
            "folder" -> c.repo.byFolder(key)
            "playlist" -> c.repo.byPlaylist(key.toLongOrNull() ?: 0L)
            else -> c.repo.favorites
        }
    }
    val songs by flow.collectAsStateWithLifecycle(initialValue = emptyList())
    val favIds by c.repo.favoriteIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val playlistName by produceState("Playlist", kind, key) {
        if (kind == "playlist") value = c.repo.playlistName(key.toLongOrNull() ?: 0L) ?: "Playlist"
    }
    val title = when (kind) {
        "album" -> songs.firstOrNull()?.album ?: "Album"
        "artist" -> key
        "folder" -> key.substringAfterLast('/').ifBlank { key }
        "playlist" -> playlistName
        else -> "Favorites"
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.text) }
            Text(title, style = MaterialTheme.typography.headlineSmall, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (songs.isEmpty()) {
            EmptyState("Nothing here yet", "Songs you add will show up in this list.")
        } else {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Button(onClick = { c.player.playSongs(songs, 0) }) { Text("Play") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { c.player.playSongs(songs, songs.indices.random(), shuffle = true) }) { Text("Shuffle") }
            }
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(songs, key = { _, s -> s.id }) { i, s ->
                    SongRow(
                        s,
                        isFavorite = s.id in favIds,
                        onClick = { c.player.playSongs(songs, i) },
                        playlistId = if (kind == "playlist") key.toLongOrNull() else null,
                    )
                }
            }
        }
    }
}
