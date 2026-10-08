package com.zhehr.auralis.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zhehr.auralis.theme.LocalAuralisPalette
import kotlinx.coroutines.launch
import java.util.Locale

fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}

@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)),
    )
}

/** Honest status label for anything not built yet. */
@Composable
fun DevStateTag(text: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(50), color = LocalAuralisPalette.current.surfaceHigh, modifier = modifier) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = LocalAuralisPalette.current.accent,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, stage: String? = null) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (stage != null) {
            Spacer(Modifier.height(16.dp))
            DevStateTag(stage)
        }
    }
}

/** Frosted-glass surface: soft shadow, translucent fill, bright top edge. Cheap: no live blur. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val p = LocalAuralisPalette.current
    Box(
        modifier
            .shadow(8.dp, shape)
            .clip(shape)
            .background(p.surface)
            .background(Brush.verticalGradient(listOf(p.text.copy(alpha = 0.10f), p.text.copy(alpha = 0.03f))))
            .border(1.dp, Brush.verticalGradient(listOf(p.text.copy(alpha = 0.28f), p.text.copy(alpha = 0.04f))), shape),
        content = content,
    )
}

/** Artwork card that tilts toward the finger and springs back. Does nothing when [enabled] is false. */
@Composable
fun TiltCard(enabled: Boolean, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .graphicsLayer {
                rotationX = rotX.value
                rotationY = rotY.value
                cameraDistance = 14f * density
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val p = event.changes.firstOrNull() ?: break
                        val nx = (p.position.x / size.width - 0.5f) * 2f
                        val ny = (p.position.y / size.height - 0.5f) * 2f
                        scope.launch {
                            rotY.snapTo(nx.coerceIn(-1f, 1f) * 12f)
                            rotX.snapTo(-ny.coerceIn(-1f, 1f) * 12f)
                        }
                    } while (event.changes.any { it.pressed })
                    scope.launch { rotX.animateTo(0f, spring()) }
                    scope.launch { rotY.animateTo(0f, spring()) }
                }
            },
        content = content,
    )
}
