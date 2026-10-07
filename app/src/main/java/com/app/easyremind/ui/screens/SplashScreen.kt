package com.app.easyremind.ui.screens

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.easyremind.ui.components.AppLogo
import kotlinx.coroutines.delay

private const val SPLASH_DURATION_MS = 1700

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var appeared by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        appeared = true
        delay(SPLASH_DURATION_MS.toLong())
        onFinished()
    }

    val logoAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
        label = "logoAlpha",
    )
    val logoScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.78f,
        animationSpec = tween(durationMillis = 650, easing = LinearEasing),
        label = "logoScale",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 260, easing = LinearEasing),
        label = "contentAlpha",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val spinnerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
        ),
        label = "spinnerRotation",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppLogo(
                size = 110.dp,
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "EASY REMIND",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(contentAlpha),
            )
            Spacer(Modifier.height(16.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(contentAlpha),
            ) {
                Text("Your schedule.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Your reminders.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Your focus.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(44.dp))
            SplashLoading(
                rotation = spinnerRotation,
                transition = infiniteTransition,
                modifier = Modifier.alpha(contentAlpha),
            )
        }
    }
}

@Composable
private fun SplashLoading(
    rotation: Float,
    transition: InfiniteTransition,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(36.dp)
                    .rotate(rotation),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
                strokeWidth = 3.dp,
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.width(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { index -> LoadingDot(index = index, transition = transition) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Loading…",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadingDot(index: Int, transition: InfiniteTransition) {
    val progress by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(
                offsetMillis = index * 150,
                offsetType = StartOffsetType.Delay,
            ),
        ),
        label = "dot$index",
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .scale(progress)
            .alpha(progress)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
    )
}
