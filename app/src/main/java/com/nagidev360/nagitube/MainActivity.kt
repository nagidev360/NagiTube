package com.nagidev360.nagitube

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class LocalVideo(val title: String, val uri: Uri, val durationMs: Long)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NagiTubeApp() }
    }
}

private fun videoPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO
    else Manifest.permission.READ_EXTERNAL_STORAGE

@Composable
private fun NagiTubeApp() {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("Home") }
    var searchVisible by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var hasVideoPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, videoPermission()) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasVideoPermission = it
    }

    LaunchedEffect(Unit) {
        if (!hasVideoPermission) permissionLauncher.launch(videoPermission())
    }

    val localVideos by produceState(initialValue = emptyList<LocalVideo>(), hasVideoPermission) {
        value = if (!hasVideoPermission) emptyList() else withContext(Dispatchers.IO) {
            val result = mutableListOf<LocalVideo>()
            val projection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DURATION)
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    result.add(LocalVideo(
                        cursor.getString(titleColumn) ?: "Local video",
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                        cursor.getLong(durationColumn)
                    ))
                }
            }
            result
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFF8AB4F8), background = Color(0xFF0F1115), surface = Color(0xFF191C22)
    )) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF0F1115)) {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).background(Color(0xFFFF1744), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlayArrow, "NagiTube logo", tint = Color.White, modifier = Modifier.size(27.dp))
                        }
                        Text(" Nagi", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Tube", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF1744))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, "Search", tint = Color.White,
                            modifier = Modifier.size(26.dp).clickable { searchVisible = !searchVisible })
                        Icon(Icons.Default.AccountCircle, "Profile", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                if (searchVisible) {
                    OutlinedTextField(
                        value = searchText, onValueChange = { searchText = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        singleLine = true, label = { Text("Search YouTube") },
                        trailingIcon = {
                            Icon(Icons.Default.Search, "Search YouTube",
                                modifier = Modifier.clickable { selectedTab = "YouTube" })
                        }
                    )
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Home", "YouTube", "Local").forEach { tab ->
                        Button(onClick = { selectedTab = tab }) { Text(tab) }
                    }
                }

                when (selectedTab) {
                    "YouTube" -> YouTubeBrowser(searchText)
                    else -> {
                        Text(
                            if (selectedTab == "Local") "Videos on your phone" else "Local videos",
                            color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 10.dp)
                        )
                        if (!hasVideoPermission) {
                            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Allow video access to show videos saved on your phone.", color = Color.LightGray)
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = { permissionLauncher.launch(videoPermission()) }) { Text("Allow access") }
                            }
                        } else if (localVideos.isEmpty()) {
                            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopCenter) {
                                Text("No local videos found on this device.", color = Color.LightGray)
                            }
                        } else {
                            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(localVideos, key = { it.uri.toString() }) { video -> LocalVideoCard(video) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YouTubeBrowser(searchText: String) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { viewContext ->
            WebView(viewContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                loadUrl("https://m.youtube.com")
            }
        },
        update = { webView ->
            val query = searchText.trim()
            if (query.isNotEmpty()) {
                val target = "https://m.youtube.com/results?search_query=" + java.net.URLEncoder.encode(query, "UTF-8")
                if (webView.url != target) webView.loadUrl(target)
            }
        }
    )
}

@Composable
private fun LocalVideoCard(video: LocalVideo) {
    val context = LocalContext.current
    val thumbnail by produceState<Bitmap?>(initialValue = null, video.uri) {
        value = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, video.uri)
                retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } catch (_: Exception) {
                null
            } finally {
                try { retriever.release() } catch (_: Exception) { }
            }
        }
    }
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp).clickable {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(video.uri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        },
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF191C22))
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(190.dp).background(Color(0xFF272C35)), contentAlignment = Alignment.Center) {
                if (thumbnail != null) {
                    Image(thumbnail!!.asImageBitmap(), contentDescription = video.title, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.PlayArrow, "Video", tint = Color(0xFF8AB4F8), modifier = Modifier.size(54.dp))
                }
                Box(Modifier.align(Alignment.BottomEnd).padding(8.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(5.dp)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                    Text(formatDuration(video.durationMs), color = Color.White, fontSize = 12.sp)
                }
            }
            Column(Modifier.padding(14.dp)) {
                Text(video.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Spacer(Modifier.height(5.dp))
                Text("On this device · Tap to play", color = Color.LightGray, fontSize = 12.sp)
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
