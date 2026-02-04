package com.ciclismo.portugal.presentation.video

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Screen that displays a YouTube video using the mobile YouTube website.
 * This approach is more reliable than the IFrame API for WebViews.
 *
 * @param videoId YouTube video ID (e.g., "dQw4w9WgXcQ")
 * @param title Video title to display
 * @param channelName Channel name to display
 * @param onBack Callback when back button is pressed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    videoId: String,
    title: String,
    channelName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Validate video ID
    val isValidVideoId = videoId.length == 11 &&
        videoId.all { it.isLetterOrDigit() || it == '_' || it == '-' }

    // Log video ID for debugging
    Log.d("VideoPlayerScreen", "Playing video ID: '$videoId' (length: ${videoId.length}, valid: $isValidVideoId)")

    // Function to open in YouTube app
    val openInYouTube = {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
        context.startActivity(intent)
    }

    // Cleanup WebView when leaving
    DisposableEffect(Unit) {
        onDispose {
            webView?.let { wv ->
                wv.loadUrl("about:blank")
                wv.destroy()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = channelName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        webView?.let { wv ->
                            wv.loadUrl("about:blank")
                            wv.destroy()
                        }
                        onBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = openInYouTube) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Abrir no YouTube",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            // YouTube Player via WebView - loads mobile YouTube site
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Show loading indicator
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color(0xFFFF0000),
                        modifier = Modifier.size(48.dp)
                    )
                }

                if (isValidVideoId) {
                    // WebView with YouTube embed for autoplay
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webView = this

                                @SuppressLint("SetJavaScriptEnabled")
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.cacheMode = WebSettings.LOAD_DEFAULT
                                settings.setSupportMultipleWindows(false)
                                settings.allowFileAccess = false

                                // Enable hardware acceleration
                                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                                webChromeClient = WebChromeClient()

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                        Log.d("VideoPlayerScreen", "WebView page loaded: $url")
                                    }
                                }

                                setBackgroundColor(android.graphics.Color.BLACK)

                                // Use embed URL with autoplay - simpler and more reliable
                                val embedHtml = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <meta charset="UTF-8">
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                        <style>
                                            * { margin: 0; padding: 0; }
                                            html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
                                            iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none; }
                                        </style>
                                    </head>
                                    <body>
                                        <iframe
                                            src="https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&rel=0&modestbranding=1&controls=1&fs=1"
                                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                            allowfullscreen>
                                        </iframe>
                                    </body>
                                    </html>
                                """.trimIndent()

                                Log.d("VideoPlayerScreen", "Loading YouTube embed with autoplay: $videoId")
                                loadDataWithBaseURL(
                                    "https://www.youtube.com",
                                    embedHtml,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Invalid video ID - show option to open in YouTube
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Vídeo não disponível",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = openInYouTube,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF0000)
                            )
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abrir no YouTube")
                        }
                    }
                }
            }

            // Bottom bar with YouTube app button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = openInYouTube,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF0000)
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Abrir na app YouTube")
                    }
                }
            }
        }
    }
}

/**
 * Extract YouTube video ID from various URL formats.
 *
 * Supports:
 * - https://www.youtube.com/watch?v=VIDEO_ID
 * - https://youtu.be/VIDEO_ID
 * - https://www.youtube.com/embed/VIDEO_ID
 * - https://www.youtube.com/shorts/VIDEO_ID
 * - https://www.youtube.com/results?search_query=... (returns null)
 *
 * @param url YouTube URL
 * @return Video ID or null if not found/invalid
 */
fun extractYouTubeVideoId(url: String): String? {
    // Search results URL - no specific video
    if (url.contains("search_query")) return null

    val patterns = listOf(
        Regex("youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11})"),
        Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),
        Regex("youtube\\.com/embed/([a-zA-Z0-9_-]{11})"),
        Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]{11})")
    )

    for (pattern in patterns) {
        val match = pattern.find(url)
        if (match != null) {
            return match.groupValues[1]
        }
    }

    return null
}
