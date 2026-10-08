package com.zhehr.auralis.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhehr.auralis.LocalContainer
import com.zhehr.auralis.data.SongEntity
import com.zhehr.auralis.ui.components.EmptyState
import com.zhehr.auralis.ui.components.ScreenHeader
import com.zhehr.auralis.ui.components.SongRow
import kotlinx.coroutines.delay

@Composable
fun SearchScreen() {
    val c = LocalContainer.current
    var query by rememberSaveable { mutableStateOf("") }
    val favIds by c.repo.favoriteIds.collectAsStateWithLifecycle(initialValue = emptySet())
    // Re-keyed on every keystroke; the delay is the debounce.
    val results by produceState(emptyList<SongEntity>(), query) {
        delay(150)
        value = if (query.isBlank()) emptyList() else c.repo.search(query.trim())
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Search")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Song, artist or album") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        )
        when {
            query.isBlank() -> EmptyState("Search your library", "Type part of a title, artist or album name.")
            results.isEmpty() -> EmptyState("No matches", "Nothing on this phone matches \"${query.trim()}\".")
            else -> LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(results, key = { _, s -> s.id }) { i, s ->
                    SongRow(s, isFavorite = s.id in favIds, onClick = { c.player.playSongs(results, i) }, highlight = query.trim())
                }
            }
        }
    }
}
