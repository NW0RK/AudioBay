package com.audiobookbay.app.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiobookbay.app.R

// Ordered dark-to-light ramps; surfaces and controls share the same tonal foundation.
internal val InkRamp = listOf(0xFF101213, 0xFF121315, 0xFF1C1E21, 0xFF2C2E2E, 0xFF373939, 0xFF747571, 0xFFCBC7BE, 0xFFE8E2D8, 0xFFF4F0E7, 0xFFFAF6EE).map { Color(it) }
internal val AmberRamp = listOf(0xFF281900, 0xFF432D00, 0xFF614100, 0xFF805500, 0xFFA87313, 0xFFD1952B, 0xFFFFB24A, 0xFFFFD995, 0xFFFFEBC7, 0xFFFFF7E9).map { Color(it) }
internal val NeutralRamp = listOf(0xFF211C14, 0xFF383026, 0xFF51473A, 0xFF6B6052, 0xFF867969, 0xFFA39785, 0xFFC1B5A2, 0xFFE0D2BE, 0xFFF0E3CF, 0xFFFFF8EF).map { Color(it) }
internal val TealRamp = listOf(0xFF00201C, 0xFF103C35, 0xFF24544B, 0xFF3D6D63, 0xFF56877C, 0xFF71A296, 0xFF8DBEB1, 0xFFA9DACD, 0xFFC5F7E9, 0xFFE8FFF7).map { Color(it) }

internal val SuccessRamp = listOf(0xFF082D16, 0xFF193B27, 0xFF28643C, 0xFF5B9B6B, 0xFF9CD5AA, 0xFFB8F1C5, 0xFFDCFBE2).map { Color(it) }
internal val ErrorRamp = listOf(0xFF410002, 0xFF690005, 0xFF93000A, 0xFFBA1A1A, 0xFFFFB4AB, 0xFFFFDAD6, 0xFFFFF8F7).map { Color(it) }

internal val DarkColorScheme = darkColorScheme(
    primaryFixed = AmberRamp[7], primaryFixedDim = AmberRamp[6], onPrimaryFixed = AmberRamp[0], onPrimaryFixedVariant = AmberRamp[2],
    secondaryFixed = NeutralRamp[7], secondaryFixedDim = NeutralRamp[6], onSecondaryFixed = NeutralRamp[0], onSecondaryFixedVariant = NeutralRamp[2],
    tertiaryFixed = TealRamp[7], tertiaryFixedDim = TealRamp[6], onTertiaryFixed = TealRamp[0], onTertiaryFixedVariant = TealRamp[2],
    primary = AmberRamp[6], onPrimary = AmberRamp[0], primaryContainer = AmberRamp[1], onPrimaryContainer = AmberRamp[7], inversePrimary = AmberRamp[3],
    secondary = NeutralRamp[6], onSecondary = NeutralRamp[0], secondaryContainer = NeutralRamp[1], onSecondaryContainer = NeutralRamp[7],
    tertiary = TealRamp[6], onTertiary = TealRamp[0], tertiaryContainer = TealRamp[1], onTertiaryContainer = TealRamp[7],
    background = InkRamp[1], onBackground = InkRamp[8], surface = InkRamp[1], onSurface = InkRamp[8],
    surfaceVariant = InkRamp[4], onSurfaceVariant = InkRamp[6], surfaceTint = AmberRamp[6],
    surfaceDim = InkRamp[1], surfaceBright = InkRamp[4], surfaceContainerLowest = InkRamp[0],
    surfaceContainerLow = InkRamp[2], surfaceContainer = InkRamp[2], surfaceContainerHigh = InkRamp[3], surfaceContainerHighest = InkRamp[4],
    inverseSurface = InkRamp[8], inverseOnSurface = InkRamp[1], outline = Color(0xFF94938D), outlineVariant = Color(0xFF494944),
    error = ErrorRamp[4], onError = ErrorRamp[1], errorContainer = ErrorRamp[2], onErrorContainer = ErrorRamp[5], scrim = Color.Black,
)
internal val LightColorScheme = lightColorScheme(
    primaryFixed = AmberRamp[7], primaryFixedDim = AmberRamp[6], onPrimaryFixed = AmberRamp[0], onPrimaryFixedVariant = AmberRamp[2],
    secondaryFixed = NeutralRamp[7], secondaryFixedDim = NeutralRamp[6], onSecondaryFixed = NeutralRamp[0], onSecondaryFixedVariant = NeutralRamp[2],
    tertiaryFixed = TealRamp[7], tertiaryFixedDim = TealRamp[6], onTertiaryFixed = TealRamp[0], onTertiaryFixedVariant = TealRamp[2],
    primary = AmberRamp[3], onPrimary = Color.White, primaryContainer = AmberRamp[7], onPrimaryContainer = AmberRamp[0], inversePrimary = AmberRamp[6],
    secondary = NeutralRamp[3], onSecondary = Color.White, secondaryContainer = NeutralRamp[7], onSecondaryContainer = NeutralRamp[0],
    tertiary = TealRamp[2], onTertiary = Color.White, tertiaryContainer = TealRamp[7], onTertiaryContainer = TealRamp[0],
    background = InkRamp[9], onBackground = InkRamp[1], surface = InkRamp[9], onSurface = InkRamp[1],
    surfaceVariant = InkRamp[7], onSurfaceVariant = Color(0xFF484842), surfaceTint = AmberRamp[3],
    surfaceDim = Color(0xFFDED8CE), surfaceBright = InkRamp[9], surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F1E7), surfaceContainer = InkRamp[8], surfaceContainerHigh = InkRamp[7], surfaceContainerHighest = Color(0xFFE2DCD2),
    inverseSurface = InkRamp[3], inverseOnSurface = InkRamp[8], outline = Color(0xFF76766E), outlineVariant = Color(0xFFCAC6BB),
    error = ErrorRamp[3], onError = Color.White, errorContainer = ErrorRamp[5], onErrorContainer = ErrorRamp[0], scrim = Color.Black,
)

@Immutable
data class StatusColors(val foreground: Color, val container: Color, val onContainer: Color)
@Immutable
data class SemanticColors(val success: StatusColors, val warning: StatusColors, val error: StatusColors)
private fun semanticColors(dark: Boolean, scheme: ColorScheme) = SemanticColors(
    success = if (dark) StatusColors(SuccessRamp[4], SuccessRamp[1], SuccessRamp[5]) else StatusColors(SuccessRamp[2], SuccessRamp[5], SuccessRamp[0]),
    warning = if (dark) StatusColors(AmberRamp[6], AmberRamp[1], AmberRamp[7]) else StatusColors(AmberRamp[3], AmberRamp[8], AmberRamp[0]),
    error = StatusColors(scheme.error, scheme.errorContainer, scheme.onErrorContainer),
)
val LocalSemanticColors = staticCompositionLocalOf { semanticColors(true, DarkColorScheme) }

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private val Manrope = FontFamily(
    Font(R.font.manrope, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.manrope, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.manrope, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.manrope, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)
private val Lora = FontFamily(Font(R.font.lora, FontWeight.Normal), Font(R.font.lora, FontWeight.Medium), Font(R.font.lora, FontWeight.SemiBold))
private fun title(size: Int, weight: FontWeight = FontWeight.Medium) = TextStyle(fontFamily = Manrope, fontWeight = weight, fontSize = size.sp, lineHeight = (size * 1.2f).sp)
private fun body(size: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(fontFamily = Manrope, fontWeight = weight, fontSize = size.sp, lineHeight = (size * 1.5f).sp)
val LiteraryTypography = Typography(
    displayLarge = title(57), displayMedium = title(45), displaySmall = title(36),
    headlineLarge = title(32, FontWeight.SemiBold), headlineMedium = title(28), headlineSmall = title(24),
    titleLarge = title(22), titleMedium = title(16, FontWeight.SemiBold), titleSmall = title(14, FontWeight.SemiBold),
    bodyLarge = body(16), bodyMedium = body(14), bodySmall = body(12),
    labelLarge = body(14, FontWeight.Medium), labelMedium = body(12, FontWeight.Medium), labelSmall = body(11, FontWeight.Medium),
)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AudioBookBayTheme(darkTheme: Boolean = true, dynamicColor: Boolean = false, fontPercent: Int = 100, content: @Composable () -> Unit) {
    val scheme = if (dynamicColor && Build.VERSION.SDK_INT >= 31) {
        if (darkTheme) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
    } else if (darkTheme) DarkColorScheme else LightColorScheme
    CompositionLocalProvider(LocalRippleConfiguration provides null, androidx.compose.foundation.LocalIndication provides com.audiobookbay.app.ui.components.QuietIndication, LocalSemanticColors provides semanticColors(darkTheme, scheme)) {
        MaterialTheme(colorScheme = scheme, typography = remember(fontPercent) { scaledTypography(fontPercent) },
            shapes = Shapes(small = RoundedCornerShape(6.dp), medium = RoundedCornerShape(10.dp), large = RoundedCornerShape(12.dp)), content = content)
    }
}

internal fun scaledTypography(percent: Int): Typography {
    val factor = percent.coerceIn(80, 150) / 100f
    fun TextStyle.scaled() = copy(fontSize = fontSize * factor, lineHeight = lineHeight * factor)
    return LiteraryTypography.copy(
        displayLarge = LiteraryTypography.displayLarge.scaled(),
        displayMedium = LiteraryTypography.displayMedium.scaled(),
        displaySmall = LiteraryTypography.displaySmall.scaled(),
        headlineLarge = LiteraryTypography.headlineLarge.scaled(),
        headlineMedium = LiteraryTypography.headlineMedium.scaled(),
        headlineSmall = LiteraryTypography.headlineSmall.scaled(),
        titleLarge = LiteraryTypography.titleLarge.scaled(),
        titleMedium = LiteraryTypography.titleMedium.scaled(),
        titleSmall = LiteraryTypography.titleSmall.scaled(),
        bodyLarge = LiteraryTypography.bodyLarge.scaled(),
        bodyMedium = LiteraryTypography.bodyMedium.scaled(),
        bodySmall = LiteraryTypography.bodySmall.scaled(),
        labelLarge = LiteraryTypography.labelLarge.scaled(),
        labelMedium = LiteraryTypography.labelMedium.scaled(),
        labelSmall = LiteraryTypography.labelSmall.scaled(),
    )
}
