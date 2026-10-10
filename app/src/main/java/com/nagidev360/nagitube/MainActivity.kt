package com.nagidev360.nagitube

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.json.JSONArray

private val Accent = Color(0xFFFF1744)
private const val PREFS = "nagitube_library"

class MainActivity : ComponentActivity() {
    @Volatile var videoOpen = false

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (videoOpen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                if (packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                    enterPictureInPictureMode(
                        PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(16, 9))
                            .build()
                    )
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NagiTubeApp() }
    }
}

@Composable
private fun NagiTubeApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, 0) }
    val scope = rememberCoroutineScope()
    var darkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", true)) }
    var tab by remember { mutableStateOf("Home") }
    var query by remember { mutableStateOf("") }
    var submittedQuery by remember { mutableStateOf("popular music videos") }
    var category by remember { mutableStateOf("All") }
    var videos by remember { mutableStateOf<List<YouTubeVideo>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedVideo by remember { mutableStateOf<YouTubeVideo?>(null) }
    var watchLater by remember { mutableStateOf(readSaved(prefs, "watch_later")) }
    var favorites by remember { mutableStateOf(readSaved(prefs, "favorites")) }
    var history by remember { mutableStateOf(readSaved(prefs, "history")) }
    var recentSearches by remember { mutableStateOf(readSaved(prefs, "recent_searches")) }
    var parentalMode by remember { mutableStateOf(prefs.getBoolean("parental_mode", false)) }

    fun runSearch(text: String, filter: String = category) {
        submittedQuery = text.trim().ifBlank { "popular music videos" }
        query = submittedQuery
        category = filter
        if (submittedQuery !in recentSearches) {
            recentSearches = (listOf(submittedQuery) + recentSearches).take(10)
            saveList(prefs, "recent_searches", recentSearches)
        }
        loading = true
        error = null
        scope.launch {
            when (val result = YouTubeDataApi.search(submittedQuery, filter)) {
                is YouTubeResult.Success -> {
                    videos = if (parentalMode) result.videos.filterNot {
                        val text = (it.title + " " + it.description).lowercase()
                        listOf("explicit", "18+", "uncensored").any(text::contains)
                    } else result.videos
                    if (videos.isEmpty()) error = "No videos found. Try another search."
                }
                is YouTubeResult.Failure -> error = result.message
            }
            loading = false
        }
    }

    fun openVideo(video: YouTubeVideo) {
        (context as? MainActivity)?.videoOpen = true
        selectedVideo = video
        history = (listOf(video.id) + history.filterNot { it == video.id }).take(50)
        saveList(prefs, "history", history)
    }

    LaunchedEffect(Unit) { runSearch("popular videos") }

    MaterialTheme(colorScheme = if (darkMode) darkColorScheme(
        primary = Accent, background = Color(0xFF0F1115), surface = Color(0xFF191C22),
        onBackground = Color.White, onSurface = Color.White
    ) else lightColorScheme(primary = Accent)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column {
                if (selectedVideo != null) {
                    VideoPlayerScreen(
                        video = selectedVideo!!,
                        saved = selectedVideo!!.id in watchLater,
                        favorite = selectedVideo!!.id in favorites,
                        onBack = { (context as? MainActivity)?.videoOpen = false; selectedVideo = null },
                        onWatchLater = {
                            watchLater = toggleSaved(watchLater, selectedVideo!!.id)
                            saveList(prefs, "watch_later", watchLater)
                        },
                        onFavorite = {
                            favorites = toggleSaved(favorites, selectedVideo!!.id)
                            saveList(prefs, "favorites", favorites)
                        }
                    )
                } else {
                    Header(onSearch = { tab = "Search" }, onProfile = { tab = "Settings" })
                    when (tab) {
                        "Search" -> {
                            SearchBar(query, { query = it }, { runSearch(query) })
                            if (recentSearches.isNotEmpty()) {
                                Text("Recent searches", Modifier.padding(start = 16.dp, top = 8.dp),
                                    color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                                Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    recentSearches.take(4).forEach { term ->
                                        SuggestionChip(onClick = { runSearch(term) }, label = { Text(term.take(18)) })
                                    }
                                }
                            }
                            Feed(videos, loading, error, category, { category = it; runSearch(submittedQuery, it) }, ::openVideo,
                                onRetry = { runSearch(submittedQuery) })
                        }
                        "Shorts" -> {
                            Text("Shorts · short videos", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Feed(videos, loading, error, "Shorts", { category = "Shorts"; runSearch("shorts", "Shorts") }, ::openVideo,
                                onRetry = { runSearch("shorts", "Shorts") })
                        }
                        "Music" -> {
                            SearchBar(query, { query = it }, { runSearch(query) })
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Hindi songs", "Telugu hits", "Tamil songs", "lofi music").forEach { term ->
                                    SuggestionChip(onClick = { runSearch(term) }, label = { Text(term) })
                                }
                            }
                            Feed(videos, loading, error, category, { category = it; runSearch(submittedQuery, it) }, ::openVideo,
                                onRetry = { runSearch(submittedQuery) })
                        }
                        "Library" -> LibraryScreen(watchLater, favorites, history, videos, ::openVideo, onClearHistory = {
                            history = emptyList(); saveList(prefs, "history", history)
                        })
                        "Settings" -> SettingsScreen(
                            darkMode, { darkMode = it; prefs.edit().putBoolean("dark_mode", it).apply() },
                            parentalMode, { parentalMode = it; prefs.edit().putBoolean("parental_mode", it).apply() }
                        )
                        else -> {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("All", "Music", "Gaming", "News", "Live").forEach { item ->
                                    FilterChip(selected = category == item, onClick = {
                                        category = item
                                        runSearch(if (item == "All") "popular videos" else item.lowercase(), item)
                                    }, label = { Text(item) })
                                }
                            }
                            Feed(videos, loading, error, category, { filter ->
                                category = filter
                                runSearch(if (filter == "All") "popular videos" else filter.lowercase(), filter)
                            }, ::openVideo, onRetry = { runSearch(submittedQuery) })
                        }
                    }
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        NavigationBarItem(selected = tab == "Home", onClick = { tab = "Home"; if (videos.isEmpty()) runSearch("popular videos") },
                            icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                        NavigationBarItem(selected = tab == "Shorts", onClick = { tab = "Shorts"; runSearch("shorts", "Shorts") },
                            icon = { Icon(Icons.Default.SmartDisplay, null) }, label = { Text("Shorts") })
                        NavigationBarItem(selected = tab == "Music", onClick = { tab = "Music"; runSearch("popular music videos") },
                            icon = { Icon(Icons.Default.LibraryMusic, null) }, label = { Text("Music") })
                        NavigationBarItem(selected = tab == "Library", onClick = { tab = "Library" },
                            icon = { Icon(Icons.Default.Bookmark, null) }, label = { Text("Library") })
                        NavigationBarItem(selected = tab == "Settings", onClick = { tab = "Settings" },
                            icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(onSearch: () -> Unit, onProfile: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(35.dp).background(Accent, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.SmartDisplay, "NagiTube", tint = Color.White)
            }
            Text(" Nagi", color = MaterialTheme.colorScheme.onBackground, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("Tube", color = Accent, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Icon(Icons.Default.Search, "Search", Modifier.clickable(onClick = onSearch), tint = MaterialTheme.colorScheme.onBackground)
            Icon(Icons.Default.AccountCircle, "Profile and settings", Modifier.clickable(onClick = onProfile),
                tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun SearchBar(value: String, onValue: (String) -> Unit, onSearch: () -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValue, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        singleLine = true, label = { Text("Search YouTube videos") }, placeholder = { Text("Song, creator, topic…") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        trailingIcon = { IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Search") } })
}

@Composable
private fun ColumnScope.Feed(
    videos: List<YouTubeVideo>, loading: Boolean, error: String?, category: String,
    onCategory: (String) -> Unit, onOpen: (YouTubeVideo) -> Unit, onRetry: () -> Unit
) {
    Column(Modifier.weight(1f, fill = true)) {
        if (category == "Shorts") {
            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = true, onClick = { onCategory("Shorts") }, label = { Text("Short videos") })
                FilterChip(selected = false, onClick = { onCategory("All") }, label = { Text("All videos") })
            }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Accent)
                    Text("Loading YouTube videos…", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onBackground)
                }
            }
            error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRetry) { Text("Try again") }
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 8.dp)) {
                items(videos, key = { it.id }) { video -> VideoCard(video, onOpen) }
            }
        }
    }
}

@Composable
private fun VideoCard(video: YouTubeVideo, onOpen: (YouTubeVideo) -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onOpen(video) }.padding(bottom = 16.dp)) {
        AsyncImage(model = video.thumbnail, contentDescription = video.title,
            modifier = Modifier.fillMaxWidth().height(215.dp).background(Color(0xFF272C35)))
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(40.dp).background(Accent, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.SmartDisplay, null, tint = Color.White)
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(video.title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                    maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text(video.channel + " · " + video.publishedAt.take(10),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun VideoPlayerScreen(
    video: YouTubeVideo, saved: Boolean, favorite: Boolean, onBack: () -> Unit,
    onWatchLater: () -> Unit, onFavorite: () -> Unit
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Now playing", fontWeight = FontWeight.Bold, fontSize = 19.sp)
        }
        AndroidView(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f), factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                loadDataWithBaseURL("https://www.youtube.com",
                    """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                    <body style="margin:0;background:#000"><iframe width="100%" height="100%" style="position:absolute;inset:0;border:0"
                    src="https://www.youtube.com/embed/${video.id}?playsinline=1&controls=1&fs=1&cc_load_policy=0&rel=0"
                    title="YouTube video player" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                    referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe></body></html>""",
                    "text/html", "UTF-8", null)
            }
        })
        Text(video.title, Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp),
            color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(video.channel, Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .7f))
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onWatchLater) { Icon(Icons.Default.Bookmark, null); Spacer(Modifier.width(5.dp)); Text(if (saved) "Saved" else "Watch later") }
            OutlinedButton(onClick = onFavorite) { Text(if (favorite) "♥ Favorite" else "♡ Favorite") }
            OutlinedButton(onClick = {
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=${video.id}")
                }
                context.startActivity(android.content.Intent.createChooser(send, "Share video"))
            }) { Icon(Icons.Default.Share, null); Text("Share") }
        }
        Text(video.description, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = .8f))
        Text("Playback uses YouTube's official embedded player. Some videos may require sign-in or be unavailable for embedding.",
            Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f), fontSize = 12.sp)
    }
}

@Composable
private fun LibraryScreen(
    watchLater: List<String>, favorites: List<String>, history: List<String>,
    currentVideos: List<YouTubeVideo>, onOpen: (YouTubeVideo) -> Unit, onClearHistory: () -> Unit
) {
    val lookup = currentVideos.associateBy { it.id }
    Column(Modifier.fillMaxSize()) {
        Text("Your library", Modifier.padding(16.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Watch later (${watchLater.size}) · Favorites (${favorites.size}) · History (${history.size})",
            Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = .7f))
        Text("Saved items appear here when their video metadata has been loaded in this session.",
            Modifier.padding(16.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f))
        OutlinedButton(onClick = onClearHistory, modifier = Modifier.padding(horizontal = 16.dp)) { Text("Clear watch history") }
        LazyColumn(Modifier.weight(1f)) {
            items((watchLater + favorites + history).distinct()) { id ->
                val video = lookup[id]
                if (video != null) VideoCard(video, onOpen)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    darkMode: Boolean, onDarkMode: (Boolean) -> Unit, parentalMode: Boolean, onParentalMode: (Boolean) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Dark theme", fontWeight = FontWeight.SemiBold)
                Text("Switch between light and dark appearance", fontSize = 12.sp)
            }
            Switch(checked = darkMode, onCheckedChange = onDarkMode)
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Basic content filter", fontWeight = FontWeight.SemiBold)
                Text("Hides results with some explicit-content keywords; not a full parental control.", fontSize = 12.sp)
            }
            Switch(checked = parentalMode, onCheckedChange = onParentalMode)
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text("Account", fontWeight = FontWeight.SemiBold)
        Text("Google sign-in and YouTube account actions are not enabled in this build.", fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Text("Privacy", fontWeight = FontWeight.SemiBold)
        Text("Search history and saved video IDs are stored locally on this device. API requests use the configured YouTube Data API key.",
            fontSize = 13.sp)
    }
}

private fun readSaved(prefs: android.content.SharedPreferences, key: String): List<String> =
    runCatching {
        val array = JSONArray(prefs.getString(key, "[]") ?: "[]")
        (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
    }.getOrDefault(emptyList())

private fun saveList(prefs: android.content.SharedPreferences, key: String, values: List<String>) {
    val array = JSONArray()
    values.forEach(array::put)
    prefs.edit().putString(key, array.toString()).apply()
}

private fun toggleSaved(values: List<String>, id: String): List<String> =
    if (id in values) values - id else (listOf(id) + values).take(100)
