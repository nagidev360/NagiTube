package com.nagidev360.nagitube

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val AuthAccent = Color(0xFFFF1744)
private const val AUTH_PREFS = "nagitube_library"

@Composable
fun NagiTubeAuthGate(content: @Composable (onLogout: () -> Unit) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(AUTH_PREFS, 0) }
    val scope = rememberCoroutineScope()
    var accessToken by remember { mutableStateOf(prefs.getString("auth_access_token", null)) }
    var refreshToken by remember { mutableStateOf(prefs.getString("auth_refresh_token", null)) }
    var email by remember { mutableStateOf(prefs.getString("auth_email", "") ?: "") }
    var checking by remember { mutableStateOf(true) }

    fun storeSession(json: JSONObject, userEmail: String) {
        accessToken = json.optString("access_token").takeIf { it.isNotBlank() }
        refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
        email = userEmail
        prefs.edit().putString("auth_access_token", accessToken)
            .putString("auth_refresh_token", refreshToken).putString("auth_email", email).apply()
    }

    LaunchedEffect(Unit) {
        val refresh = refreshToken
        if (!refresh.isNullOrBlank() && BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            val result = withContext(Dispatchers.IO) {
                runCatching { authRequest("token?grant_type=refresh_token", JSONObject().put("refresh_token", refresh)) }
            }.getOrNull()
            if (result != null && result.has("access_token")) storeSession(result, email)
            else {
                accessToken = null
                refreshToken = null
                prefs.edit().remove("auth_access_token").remove("auth_refresh_token").remove("auth_email").apply()
            }
        }
        checking = false
    }

    if (checking) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AuthAccent) }
    } else if (!accessToken.isNullOrBlank()) {
        content {
            accessToken = null
            refreshToken = null
            email = ""
            prefs.edit().remove("auth_access_token").remove("auth_refresh_token").remove("auth_email").apply()
        }
    } else {
        AuthScreen(
            onAuthenticated = { session, userEmail -> storeSession(session, userEmail) }
        )
    }
}

@Composable
private fun AuthScreen(onAuthenticated: (JSONObject, String) -> Unit) {
    val scope = rememberCoroutineScope()
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(26.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.SmartDisplay, contentDescription = "NagiTube", tint = AuthAccent, modifier = Modifier.size(58.dp))
            Text("NagiTube", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(if (isSignUp) "Create your account" else "Sign in to continue",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f), modifier = Modifier.padding(top = 6.dp, bottom = 24.dp))
            if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) {
                Text("Login setup required: add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties or GitHub Actions secrets.",
                    color = AuthAccent)
            } else {
                OutlinedTextField(
                    value = email, onValueChange = { email = it.trim() }, label = { Text("Email") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it }, label = { Text("Password") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        if (email.isBlank() || !email.contains("@") || password.length < 6) {
                            message = "Enter a valid email and a password with at least 6 characters."
                            isError = true
                        } else {
                            busy = true
                            message = null
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        authRequest(
                                            if (isSignUp) "signup" else "token?grant_type=password",
                                            JSONObject().put("email", email).put("password", password)
                                        )
                                    }
                                }
                                busy = false
                                result.onSuccess { json ->
                                    if (json.has("access_token")) {
                                        onAuthenticated(json, email)
                                    } else {
                                        message = "Account request received. Check your email to confirm, then sign in."
                                        isError = false
                                        isSignUp = false
                                    }
                                }.onFailure {
                                    message = it.message ?: "Authentication failed. Check your connection and Supabase settings."
                                    isError = true
                                }
                            }
                        }
                    },
                    enabled = !busy, modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AuthAccent)
                ) {
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text(if (isSignUp) "Create account" else "Sign in", fontSize = 16.sp)
                }
                TextButton(onClick = { isSignUp = !isSignUp; message = null }) {
                    Text(if (isSignUp) "Already have an account? Sign in" else "New to NagiTube? Create account")
                }
                message?.let {
                    Text(it, color = if (isError) AuthAccent else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp))
                }
                Text("Your account is managed securely by Supabase Auth.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                    modifier = Modifier.padding(top = 20.dp))
            }
        }
    }
}

private fun authRequest(path: String, body: JSONObject): JSONObject {
    val base = BuildConfig.SUPABASE_URL.trimEnd('/')
    val key = BuildConfig.SUPABASE_ANON_KEY
    if (base.isBlank() || key.isBlank()) error("Supabase authentication is not configured.")
    val connection = (URL("$base/auth/v1/$path").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 15000
        readTimeout = 15000
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("apikey", key)
    }
    try {
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
        if (code !in 200..299) {
            val detail = json.optString("msg").ifBlank { json.optString("message") }
                .ifBlank { json.optString("error_description") }
                .ifBlank { "Request failed (HTTP $code)." }
            error(detail)
        }
        return json
    } finally {
        connection.disconnect()
    }
}
