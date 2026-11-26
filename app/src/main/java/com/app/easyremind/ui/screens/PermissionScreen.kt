package com.app.easyremind.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.easyremind.ui.components.AppLogo
import com.app.easyremind.ui.components.animateEnter
import com.app.easyremind.ui.components.NeoButton

@Composable
fun PermissionScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onFinished() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.6f))
        AppLogo(size = 100.dp, modifier = Modifier.animateEnter(delayMs = 0))
        Spacer(Modifier.height(32.dp))
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Notifications",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp).animateEnter(delayMs = 80),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Stay on Schedule",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.animateEnter(delayMs = 120),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Receive reminders before classes\nand notifications when focus\nsessions end.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(0.9f))
        NeoButton(
            text = "Allow Notifications",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
            modifier = Modifier.animateEnter(delayMs = 200),
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onFinished()
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onFinished, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Text("Maybe Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
    }
}