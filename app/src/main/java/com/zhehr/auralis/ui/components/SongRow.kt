package com.zhehr.auralis.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhehr.auralis.LocalContainer
import com.zhehr.auralis.data.SongEntity
import com.zhehr.auralis.theme.LocalAuralisPalette
import kotlinx.coroutines.launch

fun highlighted(text: String, query: String, color: Color): AnnotatedString = buildAnnotatedString {
    val i = if (query.isBlank()) -1 else text.indexOf(query, ignoreCase = true)
    if (i < 0) {
        append(text)
    } else {
        append(text.substring(0, i))
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) { append(text.substring(i, i + query.length)) }
        append(text.substring(i + query.length))
    }
}

@Composable
fun SongRow(
    song: SongEntity,
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: String = "",
    playlistId: Long? = null,
) {
    val c = LocalContainer.current
    val palette = LocalAuralisPalette.current
    val currentId by c.player.currentId.collectAsStateWithLifecycle()
    val isCurrent = currentId == song.id
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var playlistDialog by remember { mutableStateOf(false) }

    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkImage(song.id, song.albumId, Modifier.size(48.dp), px = 144)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                highlighted(song.title, highlight, palette.accent),
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) palette.accent else palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${song.artist} · ${song.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(formatTime(song.durationMs), style = MaterialTheme.typography.labelSmall, color = palette.mutedText)
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options for ${song.title}", tint = palette.mutedText)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Play next") }, onClick = { menuOpen = false; c.player.playNext(song) })
                DropdownMenuItem(text = { Text("Add to queue") }, onClick = { menuOpen = false; c.player.addToQueue(song) })
                DropdownMenuItem(
                    text = { Text(if (isFavorite) "Remove from favorites" else "Add to favorites") },
                    onClick = { menuOpen = false; scope.launch { c.repo.setFavorite(song.id, !isFavorite) } },
                )
                DropdownMenuItem(text = { Text("Add to playlist") }, onClick = { menuOpen = false; playlistDialog = true })
                if (playlistId != null) {
                    DropdownMenuItem(
                        text = { Text("Remove from this playlist") },
                        onClick = {
                            menuOpen = false
                            scope.launch {
                                c.repo.removeFromPlaylist(playlistId, song.id)
                                Toast.makeText(ctx, "Removed from playlist", Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                }
            }
        }
    }
    if (playlistDialog) AddToPlaylistDialog(song) { playlistDialog = false }
}

@Composable
fun AddToPlaylistDialog(song: SongEntity, onDismiss: () -> Unit) {
    val repo = LocalContainer.current.repo
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val playlists by repo.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Add to playlist") },
        text = {
            Column {
                OutlinedTextField(newName, { newName = it }, label = { Text("New playlist name") }, singleLine = true)
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        scope.launch {
                            val id = repo.createPlaylist(newName.trim())
                            repo.addToPlaylist(id, song.id)
                            Toast.makeText(ctx, "Added to ${newName.trim()}", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    },
                ) { Text("Create and add") }
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(playlists, key = { it.id }) { p ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                scope.launch {
                                    val ok = repo.addToPlaylist(p.id, song.id)
                                    Toast.makeText(ctx, if (ok) "Added to ${p.name}" else "Already in ${p.name}", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            },
                        ) { Text("${p.name} (${p.songCount})", modifier = Modifier.fillMaxWidth()) }
                    }
                }
            }
        },
    )
}
