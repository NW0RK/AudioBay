package com.audiobookbay.app

import androidx.compose.ui.graphics.Color
import com.audiobookbay.app.ui.theme.DarkColorScheme
import com.audiobookbay.app.ui.theme.LightColorScheme
import com.audiobookbay.app.ui.theme.LiteraryTypography
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.*
import org.junit.Test

class LiteraryThemeTest {
    private fun luminance(color: Color): Double {
        fun linear(c: Float): Double = if (c <= 0.04045f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
    }
    @Test fun fontAdjustmentScalesEveryStyleAndLineHeight() {
        fun styles(t: androidx.compose.material3.Typography) = listOf(t.displayLarge,t.displayMedium,t.displaySmall,
            t.headlineLarge,t.headlineMedium,t.headlineSmall,t.titleLarge,t.titleMedium,t.titleSmall,
            t.bodyLarge,t.bodyMedium,t.bodySmall,t.labelLarge,t.labelMedium,t.labelSmall)
        for (percent in listOf(80, 100, 123, 150)) {
            styles(LiteraryTypography).zip(styles(com.audiobookbay.app.ui.theme.scaledTypography(percent))).forEach { (base, scaled) ->
                assertEquals(base.fontSize.value * percent / 100f, scaled.fontSize.value, 0.001f)
                assertEquals(base.lineHeight.value * percent / 100f, scaled.lineHeight.value, 0.001f)
            }
        }
    }
    @Test fun textAndContainerPairsMeetNormalTextContrast() {
        listOf(DarkColorScheme, LightColorScheme).forEach { c ->
            listOf(c.primary to c.onPrimary, c.primaryContainer to c.onPrimaryContainer,
                c.secondary to c.onSecondary, c.secondaryContainer to c.onSecondaryContainer,
                c.tertiary to c.onTertiary, c.tertiaryContainer to c.onTertiaryContainer,
                c.background to c.onBackground, c.surface to c.onSurface,
                c.surface to c.onSurfaceVariant, c.surfaceContainerHigh to c.onSurfaceVariant,
                c.error to c.onError, c.errorContainer to c.onErrorContainer).forEach { (a,b) ->
                val x = luminance(a); val y = luminance(b)
                val contrast = (max(x,y) + 0.05) / (min(x,y) + 0.05)
                assertTrue("Contrast $contrast for $a / $b", contrast >= 4.5)
            }
        }
    }
    @Test fun titleAndBodyLineSpacingUsesApprovedRatios() {
        val t = LiteraryTypography
        listOf(t.displayLarge,t.displayMedium,t.displaySmall,t.headlineLarge,t.headlineMedium,t.headlineSmall,t.titleLarge,t.titleMedium,t.titleSmall)
            .forEach { assertEquals(1.2f, it.lineHeight.value / it.fontSize.value, 0.001f) }
        listOf(t.bodyLarge,t.bodyMedium,t.bodySmall,t.labelLarge,t.labelMedium,t.labelSmall)
            .forEach { assertEquals(1.5f, it.lineHeight.value / it.fontSize.value, 0.001f) }
    }
}
