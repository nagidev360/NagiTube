package com.nagidev360.nagitube

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import org.json.JSONObject

object SupabaseAuth {
    const val CALLBACK = "nagitube://auth/callback"
    private const val PREFS = "nagitube_auth"

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun config(context: Context): Pair<String, String>? {
        val url = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val key = BuildConfig.SUPABASE_ANON_KEY.trim()
        return if (url.startsWith("https://") && key.isNotBlank()) url to key else null
    }

    fun startGoogle(context: Context): String? {
        val cfg = config(context) ?: return "Set SUPABASE_URL and SUPABASE_ANON_KEY in GitHub Actions secrets to enable sign-in."
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray())
        )
        val stateBytes = ByteArray(24).also { SecureRandom().nextBytes(it) }
        val state = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes)
        prefs(context).edit().putString("pkce_verifier", verifier).putString("oauth_state", state).apply()
        val uri = Uri.parse("${cfg.first}/auth/v1/authorize").buildUpon()
            .appendQueryParameter("provider", "google")
            .appendQueryParameter("redirect_to", CALLBACK)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "s256")
            .appendQueryParameter("state", state)
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        return null
    }

    fun handleCallback(context: Context, uri: Uri): String {
        val cfg = config(context) ?: return "Supabase is not configured in this build."
        val error = uri.getQueryParameter("error_description") ?: uri.getQueryParameter("error")
        if (error != null) return "Google sign-in failed: $error"
        val expectedState = prefs(context).getString("oauth_state", null)
        if (expectedState.isNullOrBlank() || uri.getQueryParameter("state") != expectedState) {
            return "Sign-in verification failed. Please try again."
        }
        val code = uri.getQueryParameter("code") ?: return "No sign-in code received. Please try again."
        val verifier = prefs(context).getString("pkce_verifier", null)
            ?: return "Sign-in expired. Please try again."
        prefs(context).edit().remove("oauth_state").remove("pkce_verifier").apply()
        return try {
            val conn = (URL("${cfg.first}/auth/v1/token?grant_type=pkce").openConnection() as HttpURLConnection)
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", cfg.second)
            conn.setRequestProperty("Authorization", "Bearer ${cfg.second}")
            conn.outputStream.use {
                it.write(JSONObject().put("auth_code", code).put("code_verifier", verifier).toString().toByteArray())
            }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            if (conn.responseCode !in 200..299) {
                "Sign-in failed: " + runCatching { JSONObject(body).optString("msg", JSONObject(body).optString("message", "Please check Supabase configuration.")) }.getOrDefault("Please try again.")
            } else {
                val json = JSONObject(body)
                val user = json.optJSONObject("user")
                prefs(context).edit()
                    .putString("access_token", json.optString("access_token"))
                    .putString("refresh_token", json.optString("refresh_token"))
                    .putString("email", user?.optString("email", "").orEmpty())
                    .putString("user_id", user?.optString("id", "").orEmpty())
                    .apply()
                "Signed in as " + prefs(context).getString("email", "Google user")
            }
        } catch (e: Exception) {
            "Could not finish sign-in. Check internet and Supabase settings."
        }
    }

    fun email(context: Context): String? = prefs(context).getString("email", null)?.takeIf { it.isNotBlank() }
    fun signOut(context: Context) {
        prefs(context).edit().remove("access_token").remove("refresh_token").remove("email").remove("user_id").apply()
    }
}
