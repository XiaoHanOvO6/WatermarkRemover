package demo.app.analysisapp.theme

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeTonalSpot

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * 应用主题。
 *
 * @param darkTheme 是否深色（由主题模式 跟随系统/浅色/深色 决定）
 * @param themeColor 主题色名称：["跟随系统"] + SeedColors 的 key。
 *                   "跟随系统" → Android 12+ 用系统壁纸动态取色，低版本用兜底紫；
 *                   其它 → 用 HCT 算法从种子色生成整套 MD3 配色。
 */
@Composable
fun 小寒去水印Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: String = "跟随系统",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        themeColor == "跟随系统" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        SeedColors.containsKey(themeColor) -> {
            generateColorSchemeFromSeed(SeedColors[themeColor]!!, darkTheme)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

@SuppressLint("RestrictedApi")
private fun generateColorSchemeFromSeed(
    seed: Color,
    isDark: Boolean
): ColorScheme {

    val scheme = SchemeTonalSpot(
        Hct.fromInt(seed.toArgb()),
        isDark,
        0.0
    )

    fun p(t: Int) = Color(scheme.primaryPalette.tone(t))
    fun s(t: Int) = Color(scheme.secondaryPalette.tone(t))
    fun t(tone: Int) = Color(scheme.tertiaryPalette.tone(tone))
    fun n(t: Int) = Color(scheme.neutralPalette.tone(t))
    fun nv(t: Int) = Color(scheme.neutralVariantPalette.tone(t))
    fun e(t: Int) = Color(scheme.errorPalette.tone(t))

    return ColorScheme(
        // ───────────── Primary ─────────────
        primary = p(if (isDark) 80 else 40),
        onPrimary = p(if (isDark) 20 else 100),
        primaryContainer = p(if (isDark) 30 else 90),
        onPrimaryContainer = p(if (isDark) 90 else 10),

        primaryFixed = p(90),
        primaryFixedDim = p(80),
        onPrimaryFixed = p(10),
        onPrimaryFixedVariant = p(30),

        // ───────────── Secondary ─────────────
        secondary = s(if (isDark) 80 else 40),
        onSecondary = s(if (isDark) 20 else 100),
        secondaryContainer = s(if (isDark) 30 else 90),
        onSecondaryContainer = s(if (isDark) 90 else 10),

        secondaryFixed = s(90),
        secondaryFixedDim = s(80),
        onSecondaryFixed = s(10),
        onSecondaryFixedVariant = s(30),

        // ───────────── Tertiary ─────────────
        tertiary = t(if (isDark) 80 else 40),
        onTertiary = t(if (isDark) 20 else 100),
        tertiaryContainer = t(if (isDark) 30 else 90),
        onTertiaryContainer = t(if (isDark) 90 else 10),

        tertiaryFixed = t(90),
        tertiaryFixedDim = t(80),
        onTertiaryFixed = t(10),
        onTertiaryFixedVariant = t(30),

        // ───────────── Error ─────────────
        error = e(if (isDark) 80 else 40),
        onError = e(if (isDark) 20 else 100),
        errorContainer = e(if (isDark) 30 else 90),
        onErrorContainer = e(if (isDark) 90 else 10),

        // ───────────── Surface / Background ─────────────
        background = n(if (isDark) 6 else 98),
        onBackground = n(if (isDark) 90 else 10),

        surface = n(if (isDark) 6 else 98),
        onSurface = n(if (isDark) 90 else 10),

        surfaceVariant = nv(if (isDark) 30 else 90),
        onSurfaceVariant = nv(if (isDark) 80 else 30),

        surfaceTint = p(if (isDark) 80 else 40),

        surfaceBright = n(if (isDark) 24 else 98),
        surfaceDim = n(if (isDark) 6 else 87),

        surfaceContainerLowest = n(if (isDark) 4 else 100),
        surfaceContainerLow = n(if (isDark) 10 else 96),
        surfaceContainer = n(if (isDark) 12 else 94),
        surfaceContainerHigh = n(if (isDark) 17 else 92),
        surfaceContainerHighest = n(if (isDark) 22 else 90),

        // ───────────── Outline / Misc ─────────────
        outline = nv(if (isDark) 60 else 50),
        outlineVariant = nv(if (isDark) 30 else 80),

        scrim = Color.Black,

        inverseSurface = n(if (isDark) 90 else 20),
        inverseOnSurface = n(if (isDark) 20 else 95),
        inversePrimary = p(if (isDark) 40 else 80)
    )
}
