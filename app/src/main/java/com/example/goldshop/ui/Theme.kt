package com.example.goldshop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goldshop.R

// پالت مشکی و طلایی لوکس
object Lux {
    val Black = Color(0xFF0E0B08)
    val Surface = Color(0xFF17120D)
    val Card = Color(0xFF211A12)
    val Gold = Color(0xFFD4AF37)
    val GoldLight = Color(0xFFF6E3A1)
    val GoldDeep = Color(0xFF8A6418)
    val Champagne = Color(0xFFE8D5A3)
    val Positive = Color(0xFF7BC47F)
    val goldBrush = Brush.horizontalGradient(listOf(GoldDeep, Gold, GoldLight, Gold, GoldDeep))
}

val Vazir = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_bold, FontWeight.SemiBold)
)

private val colors = darkColorScheme(
    primary = Lux.Gold,
    onPrimary = Color(0xFF1A1305),
    primaryContainer = Color(0xFF3A2F14),
    onPrimaryContainer = Lux.GoldLight,
    secondary = Lux.Champagne,
    onSecondary = Color(0xFF1A1305),
    secondaryContainer = Color(0xFF3A2F14),
    onSecondaryContainer = Lux.GoldLight,
    tertiary = Lux.GoldLight,
    background = Lux.Black,
    onBackground = Color(0xFFF3EAD3),
    surface = Lux.Surface,
    onSurface = Color(0xFFF3EAD3),
    surfaceVariant = Lux.Card,
    onSurfaceVariant = Lux.Champagne,
    surfaceTint = Lux.Gold,
    outline = Color(0xFF8C7A4B),
    outlineVariant = Color(0xFF4A3D22),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0A06),
    surfaceContainer = Lux.Surface,
    surfaceContainerHigh = Lux.Card,
    surfaceContainerHighest = Lux.Card
)

private fun style(size: Int, weight: FontWeight, line: Int) =
    TextStyle(fontFamily = Vazir, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private val typography = Typography(
    displayLarge = style(52, FontWeight.Bold, 62),
    displayMedium = style(42, FontWeight.Bold, 52),
    displaySmall = style(34, FontWeight.Bold, 44),
    headlineLarge = style(30, FontWeight.Bold, 40),
    headlineMedium = style(26, FontWeight.Bold, 36),
    headlineSmall = style(22, FontWeight.Bold, 32),
    titleLarge = style(20, FontWeight.Bold, 30),
    titleMedium = style(16, FontWeight.Medium, 26),
    titleSmall = style(14, FontWeight.Medium, 22),
    bodyLarge = style(15, FontWeight.Normal, 25),
    bodyMedium = style(14, FontWeight.Normal, 23),
    bodySmall = style(12, FontWeight.Normal, 20),
    labelLarge = style(14, FontWeight.Medium, 22),
    labelMedium = style(12, FontWeight.Medium, 18),
    labelSmall = style(11, FontWeight.Medium, 16)
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun GoldTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}

// خط طلایی گرادیانی زیر نوار بالا
@Composable
fun GoldHairline(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.5.dp)
            .background(Lux.goldBrush)
    )
}

// لوگوی الماس طلایی
@Composable
fun GemLogo(size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        fun p(x: Float, y: Float) = Offset(x / 100f * w, y / 100f * h)
        fun poly(vararg pts: Offset, brush: Brush) {
            val path = Path()
            path.moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) path.lineTo(pts[i].x, pts[i].y)
            path.close()
            drawPath(path, brush)
        }
        fun grad(a: Color, b: Color) = Brush.linearGradient(listOf(a, b), start = p(0f, 0f), end = p(100f, 100f))

        // تاج
        poly(p(8f, 36f), p(26f, 12f), p(40f, 12f), p(30f, 36f), brush = grad(Lux.GoldLight, Lux.Gold))
        poly(p(26f, 12f), p(74f, 12f), p(92f, 36f), p(8f, 36f), brush = Brush.verticalGradient(listOf(Lux.Gold, Lux.GoldLight)))
        poly(p(40f, 12f), p(60f, 12f), p(70f, 36f), p(30f, 36f), brush = grad(Lux.GoldLight, Lux.Gold))
        poly(p(60f, 12f), p(74f, 12f), p(92f, 36f), p(70f, 36f), brush = grad(Lux.Gold, Lux.GoldDeep))
        // پاویون
        poly(p(8f, 36f), p(30f, 36f), p(50f, 92f), brush = grad(Lux.Gold, Lux.GoldDeep))
        poly(p(30f, 36f), p(70f, 36f), p(50f, 92f), brush = grad(Lux.GoldLight, Lux.Gold))
        poly(p(70f, 36f), p(92f, 36f), p(50f, 92f), brush = grad(Lux.Gold, Lux.GoldDeep))
    }
}
