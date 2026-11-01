package com.audiobookbay.app.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF865E),
    secondary = Color(0xFFD1BDE5),
    tertiary = Color(0xFF7ADACD),
    background = Color(0xFF1B1533),
    surface = Color(0xFF241D3C),
    surfaceContainerLow = Color(0xFF241D3C),
    surfaceContainer = Color(0xFF302744),
    surfaceContainerHigh = Color(0xFF3A304E),
    secondaryContainer = Color(0xFF423451),
    onSecondaryContainer = Color(0xFFF4E7FF),
    outline = Color(0xFF8F819E),
    outlineVariant = Color(0xFF493D5B),
    primaryContainer = Color(0xFF513044),
    onPrimaryContainer = Color(0xFFFFDCD0),
    onBackground = Color(0xFFF8F1FF),
    onSurface = Color(0xFFF8F1FF),
    onSurfaceVariant = Color(0xFFC9BDD5),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFAC3C19),
    secondary = Color(0xFF685578),
    tertiary = Color(0xFF246B62),
    background = Color(0xFFFFF8F5),
    surface = Color(0xFFFFF8F5),
)

@Composable
fun AudioBookBayTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 39.sp, letterSpacing = 0.2.sp),
            headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 35.sp),
            titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 29.sp),
        ),
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)),
        content = content,
    )
}
