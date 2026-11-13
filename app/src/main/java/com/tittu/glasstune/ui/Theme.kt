package com.tittu.glasstune.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeMode(val label: String) {
    DARK("AMOLED Black"), AMOLED("Deep Black"), LIGHT("Charcoal")
}

val AccentNames = listOf("Silver Mist", "Ocean Blue", "Rose Quartz", "Lavender", "Mint Aqua", "Midnight")

val AccentColors = listOf(
    Color(0xFFE5EBEF), Color(0xFF7FA8C9), Color(0xFFCC96AE),
    Color(0xFFA99BCC), Color(0xFF87B8AD), Color(0xFF7886AD)
)

@Composable
fun GlassTuneTheme(mode: ThemeMode, accent: Color, content: @Composable () -> Unit) {
    val base = Color(0xFF08090E)
    val tintedBase = androidx.compose.ui.graphics.lerp(base, accent, 0.28f)
    // Each palette keeps the same frosted-silver surface and changes its tint.
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Color(0xFF08090E),
        secondary = Color(0xFFE6E8EC),
        onSecondary = Color(0xFF53616A),
        background = tintedBase,
        onBackground = Color(0xFFF1F4F6),
        surface = androidx.compose.ui.graphics.lerp(Color(0xFF24262C), accent, 0.22f),
        onSurface = Color(0xFFF1F4F6),
        surfaceVariant = androidx.compose.ui.graphics.lerp(Color(0xFF191B21), accent, 0.20f),
        onSurfaceVariant = Color(0xFFE4E6EB),
        outline = Color.White.copy(alpha = 0.38f)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun GlassBackground(
    accent: Color,
    wallpaperAsset: String? = null,
    wallpaperEnabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color(0xFF050609))) {
        if (wallpaperEnabled && wallpaperAsset != null) {
            ReetWallpaper(wallpaperAsset, darken = false, modifier = Modifier.fillMaxSize().blur(42.dp).alpha(0.46f))
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF08090E).copy(alpha = 0.68f),
                        Color(0xFF050609).copy(alpha = 0.82f),
                        Color.Black.copy(alpha = 0.92f)
                    )
                )
            )
        )
        Box(
            modifier = Modifier.fillMaxSize().drawBehind {
                val r = size.width * 0.95f
                val c1 = Offset(size.width * 0.10f, size.height * 0.08f)
                val c2 = Offset(size.width * 0.92f, size.height * 0.88f)
                drawCircle(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.10f), Color.Transparent), center = c1, radius = r),
                    radius = r, center = c1
                )
                drawCircle(
                    Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.20f), Color.Transparent), center = c2, radius = r),
                    radius = r, center = c2
                )
            },
            content = content
        )
    }
}


@Composable
fun ReetWallpaper(assetName: String, darken: Boolean = true, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    var bitmap by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    androidx.compose.runtime.LaunchedEffect(assetName) {
        val loaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { context.assets.open("backgrounds/$assetName").use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
        }
        if (loaded != null) bitmap = loaded
    }
    Box(modifier.background(Color(0xFF080910))) {
        Crossfade(bitmap, animationSpec = tween(850), label = "marvel-wallpaper") { image ->
            if (image != null) Image(image, contentDescription = "Marvel wallpaper", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        if (darken) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .48f), Color.Black.copy(alpha = .58f)))))
    }
}
