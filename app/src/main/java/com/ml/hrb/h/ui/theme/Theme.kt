// Author: Harshal R. Bisen
// Main theme configuration for the application's Compose UI.

package com.ml.hrb.h.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
    darkColorScheme(
        primary = Color.White,
        secondary = PurpleGrey80,
        tertiary = Pink80,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Color.Black,
        secondary = PurpleGrey40,
        tertiary = Pink40,
    )

val DarkGlassGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF141416), // Dark carbon/charcoal
        Color(0xFF08080A)  // Very dark slate/black
    )
)

fun Modifier.glassEffect(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    alpha: Float = 0.08f,
    borderAlpha: Float = 0.15f
): Modifier = this
    .background(Color.White.copy(alpha = alpha), shape)
    .border(1.dp, Color.White.copy(alpha = borderAlpha), shape)

@Composable
fun TRB(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    val finalColorScheme = colorScheme.copy(
        background = Color.Transparent,
        surface = Color(0xFF121214),
        onSurface = Color.White,
        onBackground = Color.White,
        primary = BrandOrange,
        onPrimary = Color.White
    )

    MaterialTheme(colorScheme = finalColorScheme, typography = Typography) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkGlassGradient)
        ) {
            content()
        }
    }
}
