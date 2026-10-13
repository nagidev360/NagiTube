package com.nagidev360.nagitube

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
internal fun LocalVideoScreen(videoUri: Uri?, onPickVideo: () -> Unit) {
    val context = LocalContext.current
    val player = remember(videoUri) {
        videoUri?.let { uri ->
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = false
            }
        }
    }
    DisposableEffect(player) {
        onDispose { player?.release() }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Local video player", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Play videos stored on your phone. Your selected file stays on-device; NagiTube does not upload it.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onPickVideo, modifier = Modifier.fillMaxWidth()) {
            Text(if (videoUri == null) "Choose a video" else "Choose another video")
        }
        if (player != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    update = { it.player = player }
                )
            }
        } else {
            Spacer(Modifier.height(4.dp))
            Text("No local video selected yet.", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
