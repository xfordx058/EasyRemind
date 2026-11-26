package com.app.easyremind.ui.components

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** True when the user disabled animations in Android accessibility settings. */
@Composable
fun reducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

/**
 * Subtle fade + upward slide used for screen entrances. The delay staggers
 * elements without making the UI feel slow. Reduced-motion settings are
 * respected: content appears immediately in that case.
 */
@Composable
fun Modifier.animateEnter(delayMs: Int = 0, durationMs: Int = 260): Modifier {
    if (reducedMotion()) return this

    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(
            durationMillis = durationMs,
            delayMillis = delayMs,
            easing = FastOutSlowInEasing,
        ),
        label = "enterAnimation",
    )

    return this
        .alpha(progress)
        .graphicsLayer { translationY = (1f - progress) * 10.dp.toPx() }
}
