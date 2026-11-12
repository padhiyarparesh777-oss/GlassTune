package com.tittu.glasstune.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Raised satin-glass / neumorphic surface used throughout the app. */
@Composable
fun Modifier.glass(shape: Shape = RoundedCornerShape(22.dp)): Modifier {
    val base = MaterialTheme.colorScheme.background
    val light = base.luminance() > 0.5f
    val topHighlight = if (light) Color.White.copy(alpha = 0.58f) else Color.White.copy(alpha = 0.20f)
    val lowerShade = if (light) Color(0xFF89969E).copy(alpha = 0.10f) else Color(0xFF26323A).copy(alpha = 0.28f)
    val edgeLight = if (light) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.48f)
    val edgeDark = if (light) Color(0xFF71808A).copy(alpha = 0.28f) else Color(0xFF26323A).copy(alpha = 0.72f)

    return this
        .shadow(13.dp, shape, clip = false, ambientColor = Color(0xFF26323A).copy(alpha = 0.30f), spotColor = Color.White.copy(alpha = 0.32f))
        .clip(shape)
        .background(
            Brush.linearGradient(
                colors = listOf(topHighlight, base.copy(alpha = 0.78f), lowerShade),
                start = androidx.compose.ui.geometry.Offset.Zero,
                end = androidx.compose.ui.geometry.Offset(900f, 900f)
            ),
            shape
        )
        .border(1.dp, Brush.linearGradient(listOf(edgeLight, edgeDark, edgeLight)), shape)
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = MaterialTheme.colorScheme.onBackground
) {
    Box(
        modifier = modifier.size(size).glass(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint)
    }
}

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
