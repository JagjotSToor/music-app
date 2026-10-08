package com.zhehr.auralis.ui.components

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.zhehr.auralis.LocalContainer
import com.zhehr.auralis.R
import com.zhehr.auralis.theme.Contrast
import com.zhehr.auralis.theme.LocalAuralisPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class ArtColors(val accent: Color, val tint: Color)

/**
 * Loads, downsamples and caches artwork. Bitmaps live in a size-bounded LruCache, songs without
 * artwork are remembered so we do not retry them, and album art is shared between its songs.
 */
class ArtworkLoader(private val context: Context) {
    private val bitmaps = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val missing: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val colorCache = ConcurrentHashMap<String, ArtColors>()
    private val blurCache = ConcurrentHashMap<String, Bitmap>()

    private fun key(songId: Long, albumId: Long) = if (albumId > 0) "a$albumId" else "s$songId"

    suspend fun load(songId: Long, albumId: Long, px: Int): Bitmap? = withContext(Dispatchers.IO) {
        val k = "${key(songId, albumId)}@$px"
        bitmaps.get(k)?.let { return@withContext it }
        if (k in missing) return@withContext null
        val bmp = decode(songId, albumId, px)
        if (bmp != null) bitmaps.put(k, bmp) else missing.add(k)
        bmp
    }

    private fun decode(songId: Long, albumId: Long, px: Int): Bitmap? {
        val songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
        val primary = runCatching {
            if (Build.VERSION.SDK_INT >= 29) context.contentResolver.loadThumbnail(songUri, Size(px, px), null)
            else legacy(albumId)
        }.getOrNull()
        return primary ?: embedded(songUri)
    }

    private fun legacy(albumId: Long): Bitmap? {
        val uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }

    private fun embedded(uri: Uri): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(context, uri)
            r.embeddedPicture?.let { bytes ->
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            }
        } catch (e: Exception) {
            null
        } finally {
            runCatching { r.release() }
        }
    }

    private fun software(b: Bitmap): Bitmap =
        if (b.config == Bitmap.Config.HARDWARE) b.copy(Bitmap.Config.ARGB_8888, false) else b

    /** Accent colour pulled from the artwork and nudged until it is readable on near-black. */
    suspend fun colors(songId: Long, albumId: Long): ArtColors? {
        val k = key(songId, albumId)
        colorCache[k]?.let { return it }
        val bmp = software(load(songId, albumId, 256) ?: return null)
        val result = withContext(Dispatchers.Default) {
            val palette = Palette.from(bmp).maximumColorCount(16).generate()
            val swatch = palette.vibrantSwatch ?: palette.lightVibrantSwatch
                ?: palette.dominantSwatch ?: palette.mutedSwatch
            val base = swatch?.rgb ?: return@withContext null
            val dark = Color(0xFF0B0D12)
            var accent = base
            var tries = 0
            while (Contrast.ratio(Color(accent), dark) < Contrast.AA_NORMAL && tries < 10) {
                accent = ColorUtils.blendARGB(accent, 0xFFFFFFFF.toInt(), 0.12f)
                tries++
            }
            ArtColors(Color(accent), Color(ColorUtils.blendARGB(base, 0xFF000000.toInt(), 0.55f)))
        }
        if (result != null) colorCache[k] = result
        return result
    }

    /** Tiny bitmap that is stretched over the screen; bilinear upscaling gives a cheap, cached blur. */
    suspend fun blurred(songId: Long, albumId: Long): Bitmap? {
        val k = key(songId, albumId)
        blurCache[k]?.let { return it }
        val src = software(load(songId, albumId, 256) ?: return null)
        val small = withContext(Dispatchers.Default) { Bitmap.createScaledBitmap(src, 24, 24, true) }
        blurCache[k] = small
        return small
    }
}

@Composable
fun ArtworkImage(
    songId: Long,
    albumId: Long,
    modifier: Modifier = Modifier,
    px: Int = 256,
    shape: Shape = RoundedCornerShape(10.dp),
) {
    val loader = LocalContainer.current.artwork
    val palette = LocalAuralisPalette.current
    val bmp by produceState<Bitmap?>(null, songId, albumId, px) { value = loader.load(songId, albumId, px) }
    Box(modifier.clip(shape).background(palette.surfaceHigh), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) {
            val img = remember(b) { b.asImageBitmap() }
            Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(
                painterResource(R.drawable.ic_library),
                contentDescription = null,
                tint = palette.mutedText.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxSize(0.4f),
            )
        }
    }
}
