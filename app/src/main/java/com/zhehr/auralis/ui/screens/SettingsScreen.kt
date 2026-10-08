package com.zhehr.auralis.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhehr.auralis.BuildConfig
import com.zhehr.auralis.LocalContainer
import com.zhehr.auralis.theme.Contrast
import com.zhehr.auralis.theme.LocalAuralisPalette
import com.zhehr.auralis.theme.ThemeMode
import com.zhehr.auralis.ui.components.ScreenHeader
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsScreen() {
    val c = LocalContainer.current
    val palette = LocalAuralisPalette.current
    val scope = rememberCoroutineScope()
    val themeMode by c.settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val reduce by c.settings.reduceMotion.collectAsStateWithLifecycle(initialValue = false)
    val scan by c.scanner.state.collectAsStateWithLifecycle()
    val count by c.repo.count.collectAsStateWithLifecycle(initialValue = 0)
    val textRatio = Contrast.ratio(palette.text, palette.background)
    val mutedRatio = Contrast.ratio(palette.mutedText, palette.background)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Settings")

        Section("Appearance")
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == themeMode,
                    onClick = { scope.launch { c.settings.setThemeMode(mode) } },
                    label = { Text(mode.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.accent,
                        selectedLabelColor = palette.onAccent,
                    ),
                )
            }
        }
        Note("Contrast check: text ${fmt(textRatio)}:1, muted text ${fmt(mutedRatio)}:1 " +
            if (Contrast.meetsAA(palette.mutedText, palette.background)) "(passes AA)" else "(fails AA)")
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Reduce motion", style = MaterialTheme.typography.titleMedium, color = palette.text)
                Text("Turns off artwork tilt and shortens transitions.", style = MaterialTheme.typography.bodyMedium, color = palette.mutedText)
            }
            Switch(checked = reduce, onCheckedChange = { scope.launch { c.settings.setReduceMotion(it) } })
        }

        Section("Library")
        Note("$count songs in your library")
        Note(scan.message.ifBlank { "Not scanned yet in this session." })
        Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Button(enabled = !scan.running, onClick = { scope.launch { c.scanner.scan() } }) {
                Text(if (scan.running) "Scanning…" else "Rescan")
            }
        }

        Section("Privacy")
        Note("AURALIS has no internet permission and no microphone permission. Your library is stored only on this phone.")

        Section("About")
        Text("AURALIS", style = MaterialTheme.typography.titleMedium, color = palette.text, modifier = Modifier.padding(horizontal = 20.dp))
        Note("by ZHEHR · version ${BuildConfig.VERSION_NAME}")
        Note("Not built yet: equalizer and audio effects, visualizer, lyrics, tag editing, Theme Studio, online sources.")
    }
}

private fun fmt(v: Float) = String.format(Locale.US, "%.1f", v)

@Composable
private fun Section(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = LocalAuralisPalette.current.accent,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
    )
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = LocalAuralisPalette.current.mutedText,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
}
