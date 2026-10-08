package com.zhehr.auralis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.zhehr.auralis.LocalContainer
import com.zhehr.auralis.R
import com.zhehr.auralis.data.PlaylistRow
import com.zhehr.auralis.theme.LocalAuralisPalette
import com.zhehr.auralis.ui.components.ArtworkImage
import com.zhehr.auralis.ui.components.EmptyState
import com.zhehr.auralis.ui.components.ScreenHeader
import com.zhehr.auralis.ui.components.SongRow
import kotlinx.coroutines.launch

private val TABS = listOf("Songs", "Albums", "Artists", "Folders", "Playlists")

@Composable
fun LibraryScreen(nav: NavHostController) {
    val palette = LocalAuralisPalette.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Library")
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(TABS) { i, name ->
                FilterChip(
                    selected = tab == i,
                    onClick = { tab = i },
                    label = { Text(name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.accent,
                        selectedLabelColor = palette.onAccent,
                    ),
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                0 -> SongsTab()
                1 -> AlbumsTab(nav)
                2 -> ArtistsTab(nav)
                3 -> FoldersTab(nav)
                else -> PlaylistsTab(nav)
            }
        }
    }
}

@Composable
private fun ScanEmpty() {
    val scan by LocalContainer.current.scanner.state.collectAsStateWithLifecycle()
    EmptyState(
        title = if (scan.running) "Scanning your music…" else "Nothing here yet",
        body = if (scan.running) "Your library will appear as soon as it is ready."
        else "Add audio files to this phone, then tap Rescan in Settings.",
    )
}

@Composable
private fun SongsTab() {
    val c = LocalContainer.current
    val palette = LocalAuralisPalette.current
    val songs by c.repo.allSongs.collectAsStateWithLifecycle(initialValue = emptyList())
    val favIds by c.repo.favoriteIds.collectAsStateWithLifecycle(initialValue = emptySet())
    var sortIdx by rememberSaveable { mutableIntStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }
    val sorts = listOf("Title", "Artist", "Recently added", "Longest")
    val sorted = remember(songs, sortIdx) {
        when (sortIdx) {
            1 -> songs.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
            2 -> songs.sortedByDescending { it.dateAdded }
            3 -> songs.sortedByDescending { it.durationMs }
            else -> songs
        }
    }
    if (songs.isEmpty()) {
        ScanEmpty()
        return
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${songs.size} songs", style = MaterialTheme.typography.labelLarge, color = palette.mutedText, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { c.player.playSongs(sorted, (0..sorted.lastIndex).random(), shuffle = true) }) { Text("Shuffle") }
            Spacer(Modifier.width(8.dp))
            Box {
                TextButton(onClick = { sortMenu = true }) { Text("Sort: ${sorts[sortIdx]}") }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    sorts.forEachIndexed { i, s -> DropdownMenuItem(text = { Text(s) }, onClick = { sortIdx = i; sortMenu = false }) }
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(sorted, key = { _, s -> s.id }) { i, s ->
                SongRow(s, isFavorite = s.id in favIds, onClick = { c.player.playSongs(sorted, i) })
            }
        }
    }
}

@Composable
private fun AlbumsTab(nav: NavHostController) {
    val c = LocalContainer.current
    val palette = LocalAuralisPalette.current
    val albums by c.repo.albums.collectAsStateWithLifecycle(initialValue = emptyList())
    if (albums.isEmpty()) { ScanEmpty(); return }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(albums, key = { "${it.albumId}-${it.album}" }) { a ->
            Column(Modifier.clickable { nav.navigate("detail/album/${a.albumId}") }) {
                ArtworkImage(a.songId, a.albumId, Modifier.fillMaxWidth().aspectRatio(1f), px = 360, shape = RoundedCornerShape(16.dp))
                Text(a.album, style = MaterialTheme.typography.labelLarge, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                Text("${a.artist} · ${a.songCount}", style = MaterialTheme.typography.bodyMedium, color = palette.mutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ArtistsTab(nav: NavHostController) {
    val c = LocalContainer.current
    val palette = LocalAuralisPalette.current
    val artists by c.repo.artists.collectAsStateWithLifecycle(initialValue = emptyList())
    if (artists.isEmpty()) { ScanEmpty(); return }
    LazyColumn(Modifier.fillMaxSize()) {
        items(artists.size, key = { artists[it].artist }) { i ->
            val a = artists[i]
            Row(
                Modifier.fillMaxWidth().clickable { nav.navigate("detail/artist/${android.net.Uri.encode(a.artist)}") }.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArtworkImage(a.songId, a.albumId, Modifier.size(48.dp), px = 144, shape = RoundedCornerShape(24.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(a.artist, style = MaterialTheme.typography.titleMedium, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${a.albumCount} albums · ${a.songCount} songs", style = MaterialTheme.typography.bodyMedium, color = palette.mutedText)
                }
            }
        }
    }
}

@Composable
private fun FoldersTab(nav: NavHostController) {
    val c = LocalContainer.current
    val palette = LocalAuralisPalette.current
    val folders by c.repo.folders.collectAsStateWithLifecycle(initialValue = emptyList())
    if (folders.isEmpty()) { ScanEmpty(); return }
    LazyColumn(Modifier.fillMaxSize()) {
        items(folders.size, key = { folders[it].folder }) { i ->
            val f = folders[i]
            Column(Modifier.fillMaxWidth().clickable { nav.navigate("detail/folder/${android.net.Uri.encode(f.folder)}") }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text(f.folder.substringAfterLast('/').ifBlank { f.folder }, style = MaterialTheme.typography.titleMedium, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${f.folder} · ${f.songCount} songs", style = MaterialTheme.typography.bodyMedium, color = palette.mutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PlaylistsTab(nav: NavHostController) {
    val repo = LocalContainer.current.repo
    val palette = LocalAuralisPalette.current
    val scope = rememberCoroutineScope()
    val playlists by repo.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<PlaylistRow?>(null) }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                Modifier.fillMaxWidth().clickable { nav.navigate("detail/favorites/0") }.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = palette.accent)
                Spacer(Modifier.width(16.dp))
                Text("Favorites", style = MaterialTheme.typography.titleMedium, color = palette.text)
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().clickable { creating = true }.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = palette.accent)
                Spacer(Modifier.width(16.dp))
                Text("New playlist", style = MaterialTheme.typography.titleMedium, color = palette.text)
            }
        }
        items(playlists, key = { it.id }) { p ->
            Row(
                Modifier.fillMaxWidth().clickable { nav.navigate("detail/playlist/${p.id}") }.padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_queue), contentDescription = null, tint = palette.mutedText)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${p.songCount} songs", style = MaterialTheme.typography.bodyMedium, color = palette.mutedText)
                }
                IconButton(onClick = { toDelete = p }) { Icon(Icons.Default.Delete, contentDescription = "Delete ${p.name}", tint = palette.mutedText) }
            }
        }
    }

    if (creating) {
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text("New playlist") },
            text = { OutlinedTextField(newName, { newName = it }, singleLine = true, label = { Text("Name") }) },
            confirmButton = {
                TextButton(enabled = newName.isNotBlank(), onClick = {
                    scope.launch { repo.createPlaylist(newName.trim()) }
                    newName = ""
                    creating = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text("Cancel") } },
        )
    }
    toDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete \"${p.name}\"?") },
            text = { Text("The playlist is removed. Your songs are not deleted.") },
            confirmButton = { TextButton(onClick = { scope.launch { repo.deletePlaylist(p.id) }; toDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}
