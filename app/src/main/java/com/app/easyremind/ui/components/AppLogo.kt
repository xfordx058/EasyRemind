package com.app.easyremind.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.easyremind.R
import com.app.easyremind.ui.theme.HeroShape

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    shape: Shape = HeroShape,
) {
    val (dark, light) = neoShadowColors()
    Box(
        modifier = modifier
            .size(size)
            .softCard(shape, dark, light, offset = 6.dp, elevation = 12.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary, shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.easy_remind_app_icon),
            contentDescription = "Easy Remind logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size),
        )
    }
}
