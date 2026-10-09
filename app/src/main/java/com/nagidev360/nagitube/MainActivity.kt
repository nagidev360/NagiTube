package com.nagidev360.nagitube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class VideoItem(val title: String, val channel: String, val duration: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NagiTubeApp() }
    }
}

@Composable
private fun NagiTubeApp() {
    val videos = listOf(
        VideoItem("Welcome to NagiTube", "NagiTube Studio · Sample", "0:30"),
        VideoItem("Your video library starts here", "Local / authorized media", "—"),
        VideoItem("Build your own channel", "Creator guide · Sample", "—")
    )
    var selectedTab by remember { mutableStateOf("Home") }
    var showSearch by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFF8AB4F8),
        background = Color(0xFF0F1115),
        surface = Color(0xFF191C22)
    )) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0F1115)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color(0xFF8AB4F8))
                        Text(" NagiTube", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White,
                            modifier = Modifier.clickable { showSearch = !showSearch })
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Color.White)
                    }
                }
                if (showSearch) {
                    Text("Search will be connected when the video catalog is added.", color = Color.LightGray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                Row(modifier = Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Home", "Shorts", "Subscriptions").forEach { tab ->
                        Button(onClick = { selectedTab = tab }) { Text(tab) }
                    }
                }
                Text(selectedTab, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 10.dp))
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(videos) { video ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF191C22))
                        ) {
                            Column {
                                Column(
                                    modifier = Modifier.fillMaxWidth().height(170.dp)
                                        .background(Color(0xFF272C35)),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = "Video placeholder",
                                        tint = Color(0xFF8AB4F8), modifier = Modifier.height(48.dp))
                                    Text("Video thumbnail", color = Color.LightGray)
                                }
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(video.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                                    Spacer(Modifier.height(5.dp))
                                    Text("${video.channel}  ·  ${video.duration}", color = Color.LightGray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    item {
                        Text("Starter preview only — playback, Google login, and server are not connected yet.",
                            color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}
