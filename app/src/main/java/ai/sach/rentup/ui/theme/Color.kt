package ai.sach.rentup.ui.theme

import androidx.compose.ui.graphics.Color

/** Shared RentUp brand and semantic colors. */
object RentUpColors {
    val PrimaryTerracotta = Color(0xFFC65D42)
    val PrimaryTerracottaLight = Color(0xFFF5DED6)
    val PrimaryTerracottaDark = Color(0xFF8F3928)

    val Ink = Color(0xFF202322)
    val InkMuted = Color(0xFF68706D)
    val Canvas = Color(0xFFF7F5F1)
    val Surface = Color(0xFFFFFEFB)
    val SurfaceAlt = Color(0xFFEDEAE4)
    val Border = Color(0xFFE1DDD5)

    val DarkCanvas = Color(0xFF151716)
    val DarkSurface = Color(0xFF202321)
    val DarkSurfaceAlt = Color(0xFF2A2D2B)
    val DarkBorder = Color(0xFF3B403D)
    val DarkText = Color(0xFFF3F1EB)
    val DarkMuted = Color(0xFFB9BDB8)

    val Success = Color(0xFF3B8064)
    val Warning = Color(0xFFB8782B)
    val Error = Color(0xFFB7473B)
    val Info = Color(0xFF47758C)

    // Backwards-compatible aliases used by existing screens.
    val PrimaryGreen = PrimaryTerracotta
    val PrimaryGreenLight = PrimaryTerracottaLight
    val PrimaryGreenDark = PrimaryTerracottaDark
    val BackgroundGray = Canvas
    val White = Color.White
    val Black = Color.Black
    val Gray = InkMuted
    val LightGray = Border
    val LightBackground = SurfaceAlt
    val DarkBackground = DarkCanvas
    val Red = Error
}

val Terracotta = RentUpColors.PrimaryTerracotta
val Canvas = RentUpColors.Canvas
val Ink = RentUpColors.Ink
val Surface = RentUpColors.Surface
val Border = RentUpColors.Border
val DarkCanvas = RentUpColors.DarkCanvas
val DarkSurface = RentUpColors.DarkSurface
val DarkText = RentUpColors.DarkText
val DarkMuted = RentUpColors.DarkMuted
val Success = RentUpColors.Success
val Warning = RentUpColors.Warning
val Error = RentUpColors.Error
val Info = RentUpColors.Info

fun rentUpColorScheme(primaryColor: Color, darkTheme: Boolean): androidx.compose.material3.ColorScheme =
    if (darkTheme) {
        androidx.compose.material3.darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = RentUpColors.PrimaryTerracottaDark,
            onPrimaryContainer = Color.White,
            secondary = RentUpColors.Success,
            onSecondary = Color.White,
            background = RentUpColors.DarkCanvas,
            onBackground = RentUpColors.DarkText,
            surface = RentUpColors.DarkSurface,
            onSurface = RentUpColors.DarkText,
            surfaceVariant = RentUpColors.DarkSurfaceAlt,
            onSurfaceVariant = RentUpColors.DarkMuted,
            outline = RentUpColors.DarkBorder,
            outlineVariant = RentUpColors.DarkBorder,
            error = RentUpColors.Error,
            onError = Color.White
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = RentUpColors.PrimaryTerracottaLight,
            onPrimaryContainer = RentUpColors.PrimaryTerracottaDark,
            secondary = RentUpColors.Success,
            onSecondary = Color.White,
            background = RentUpColors.Canvas,
            onBackground = RentUpColors.Ink,
            surface = RentUpColors.Surface,
            onSurface = RentUpColors.Ink,
            surfaceVariant = RentUpColors.SurfaceAlt,
            onSurfaceVariant = RentUpColors.InkMuted,
            outline = RentUpColors.Border,
            outlineVariant = RentUpColors.Border,
            error = RentUpColors.Error,
            onError = Color.White
        )
    }

// Legacy name retained for existing callers.
val rentUpPrimaryColor: Color get() = RentUpColors.PrimaryTerracotta

fun colorFromHexOrDefault(value: String, fallback: Color): Color = try {
    Color(android.graphics.Color.parseColor(value))
} catch (_: Exception) {
    fallback
}

// Existing code references these names in older files.
val PrimaryGreen: Color get() = RentUpColors.PrimaryTerracotta
val PrimaryGreenLight: Color get() = RentUpColors.PrimaryTerracottaLight
val PrimaryGreenDark: Color get() = RentUpColors.PrimaryTerracottaDark
val BackgroundGray: Color get() = RentUpColors.Canvas
val DarkBackground: Color get() = RentUpColors.DarkCanvas
val DarkSurface: Color get() = RentUpColors.DarkSurface
val DarkMuted: Color get() = RentUpColors.DarkMuted
val DarkBorder: Color get() = RentUpColors.DarkBorder
val Red: Color get() = RentUpColors.Error
val Gray: Color get() = RentUpColors.InkMuted
val LightGray: Color get() = RentUpColors.Border
val LightBackground: Color get() = RentUpColors.SurfaceAlt
val White: Color get() = Color.White
val Black: Color get() = Color.Black
