package com.tc128.giamdinhnative.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Bảng màu thương hiệu GiamDinh/TC128 (kế thừa từ app Xamarin gốc)
val BrandPrimary = Color(0xFF5BA9FC)
val BrandAccent  = Color(0xFF96D1FF)
val BrandWarning = Color(0xFFFC5BA6)

val Navy900   = Color(0xFF0A1628)
val Navy800   = Color(0xFF0D2137)
val Navy700   = Color(0xFF2E6FB8)
val Navy600   = Color(0xFF3D86D6)
val Navy500   = Color(0xFF5BA9FC)
val NavyLight = Color(0xFFE6F2FF)

val Teal500   = Color(0xFF00897B)
val TealLight = Color(0xFFE0F2F1)

// Status colors
val StatusWaiting  = Color(0xFFF59E0B)  // amber
val StatusProgress = Color(0xFF3B82F6)  // blue
val StatusDone     = Color(0xFF10B981)  // green
val StatusReject   = Color(0xFFEF4444)  // red

private val LightColorScheme = lightColorScheme(
    primary          = Navy700,
    onPrimary        = Color.White,
    primaryContainer = NavyLight,
    onPrimaryContainer = Navy700,
    secondary        = Teal500,
    onSecondary      = Color.White,
    secondaryContainer = TealLight,
    onSecondaryContainer = Teal500,
    background       = Color(0xFFF1F4F9),
    onBackground     = Color(0xFF0D1B2A),
    surface          = Color.White,
    onSurface        = Color(0xFF0D1B2A),
    surfaceVariant   = Color(0xFFEEF2F8),
    onSurfaceVariant = Color(0xFF4A5568),
    outline          = Color(0xFFCBD5E1),
    outlineVariant   = Color(0xFFE2E8F0),
    error            = Color(0xFFDC2626),
    onError          = Color.White,
    errorContainer   = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
)

@Composable
fun GiamDinhNativeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
