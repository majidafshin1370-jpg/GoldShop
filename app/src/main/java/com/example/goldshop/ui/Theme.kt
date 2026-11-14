package com.example.goldshop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.RowScope
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
        fun poly(brush: Brush, vararg xy: Float) {
            val path = Path()
            path.moveTo(xy[0] / 100f * w, xy[1] / 100f * h)
            var i = 2
            while (i < xy.size) {
                path.lineTo(xy[i] / 100f * w, xy[i + 1] / 100f * h)
                i += 2
            }
            path.close()
            drawPath(path, brush)
        }
        fun grad(a: Color, b: Color) = Brush.linearGradient(listOf(a, b), start = p(0f, 0f), end = p(100f, 100f))

        // تاج
        poly(grad(Lux.GoldLight, Lux.Gold), 8f, 36f, 26f, 12f, 40f, 12f, 30f, 36f)
        poly(Brush.verticalGradient(listOf(Lux.Gold, Lux.GoldLight)), 26f, 12f, 74f, 12f, 92f, 36f, 8f, 36f)
        poly(grad(Lux.GoldLight, Lux.Gold), 40f, 12f, 60f, 12f, 70f, 36f, 30f, 36f)
        poly(grad(Lux.Gold, Lux.GoldDeep), 60f, 12f, 74f, 12f, 92f, 36f, 70f, 36f)
        // پاویون
        poly(grad(Lux.Gold, Lux.GoldDeep), 8f, 36f, 30f, 36f, 50f, 92f)
        poly(grad(Lux.GoldLight, Lux.Gold), 30f, 36f, 70f, 36f, 50f, 92f)
        poly(grad(Lux.Gold, Lux.GoldDeep), 70f, 36f, 92f, 36f, 50f, 92f)
    }
}


// کارت لوکس با حاشیه طلایی گرادیانی
@Composable
fun LuxCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Lux.Card),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(Lux.Gold.copy(alpha = 0.85f), Lux.GoldDeep.copy(alpha = 0.25f), Lux.Gold.copy(alpha = 0.6f)))
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        content = content
    )
}

// دکمه اصلی با گرادیان طلایی
@Composable
fun GoldButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val dark = Color(0xFF1A1305)
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = dark,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = dark.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) Lux.goldBrush else Brush.horizontalGradient(listOf(Color(0xFF4A3D22), Color(0xFF5E4E2A))),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                content = content
            )
        }
    }
}

@Composable
fun luxNavColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Lux.GoldLight,
    selectedTextColor = Lux.Gold,
    indicatorColor = Color(0xFF3A2F14),
    unselectedIconColor = Lux.Champagne.copy(alpha = 0.6f),
    unselectedTextColor = Lux.Champagne.copy(alpha = 0.6f)
)

@Composable
fun NavLabel(text: String) {
    androidx.compose.material3.Text(
        text,
        fontSize = 10.sp,
        maxLines = 1,
        softWrap = false,
        fontWeight = FontWeight.Medium
    )
}
