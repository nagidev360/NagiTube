package com.nagidev360.nagitube

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal data class YouTubeVideo(
    val id: String,
    val title: String,
    val channel: String,
    val thumbnail: String,
    val publishedAt: String,
    val description: String
)

internal sealed interface YouTubeResult {
    data class Success(val videos: List<YouTubeVideo>) : YouTubeResult
    data class Failure(val message: String) : YouTubeResult
}

internal object YouTubeDataApi {
    suspend fun search(query: String, category: String = "all"): YouTubeResult =
        withContext(Dispatchers.IO) {
            val key = BuildConfig.YOUTUBE_API_KEY
            if (key.isBlank()) {
                return@withContext YouTubeResult.Failure(
                    "YouTube API key not configured. Add YOUTUBE_API_KEY to local.properties."
                )
            }
            var connection: HttpURLConnection? = null
            try {
                val params = Uri.Builder()
                    .appendQueryParameter("part", "snippet")
                    .appendQueryParameter("type", "video")
                    .appendQueryParameter("videoEmbeddable", "true")
                    .appendQueryParameter("safeSearch", "moderate")
                    .appendQueryParameter("maxResults", "25")
                    .appendQueryParameter("q", query.ifBlank { "popular music" })
                    .appendQueryParameter("key", key)
                when (category) {
                    "Shorts" -> params.appendQueryParameter("videoDuration", "short")
                    "Live" -> params.appendQueryParameter("eventType", "live")
                }
                connection = (URL("https://www.googleapis.com/youtube/v3/search?${params.build().encodedQuery}")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 12000
                    readTimeout = 12000
                    setRequestProperty("Accept", "application/json")
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream.bufferedReader().use { it.readText() }
                if (status !in 200..299) {
                    val apiMessage = runCatching {
                        JSONObject(body).getJSONObject("error").getString("message")
                    }.getOrNull()
                    return@withContext YouTubeResult.Failure(
                        apiMessage ?: "YouTube request failed (HTTP $status). Check API key and quota."
                    )
                }
                val items = JSONObject(body).optJSONArray("items")
                val videos = buildList {
                    if (items != null) for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val id = item.optJSONObject("id")?.optString("videoId").orEmpty()
                        if (id.isBlank() || id == "null") continue
                        val snippet = item.optJSONObject("snippet") ?: continue
                        val thumbnails = snippet.optJSONObject("thumbnails")
                        val image = thumbnails?.optJSONObject("high")?.optString("url")
                            ?.takeIf { !it.isNullOrBlank() }
                            ?: thumbnails?.optJSONObject("medium")?.optString("url").orEmpty()
                        add(
                            YouTubeVideo(
                                id = id,
                                title = snippet.optString("title", "Untitled video"),
                                channel = snippet.optString("channelTitle", "YouTube channel"),
                                thumbnail = image,
                                publishedAt = snippet.optString("publishedAt", ""),
                                description = snippet.optString("description", "")
                            )
                        )
                    }
                }
                YouTubeResult.Success(videos)
            } catch (e: Exception) {
                YouTubeResult.Failure(
                    if (e is java.net.SocketTimeoutException) "Connection timed out. Try again."
                    else "Could not load YouTube videos. Check your internet connection."
                )
            } finally {
                connection?.disconnect()
            }
        }
}
