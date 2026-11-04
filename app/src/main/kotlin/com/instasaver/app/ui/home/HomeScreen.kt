package com.instasaver.app.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.instasaver.app.core.download.DownloadQueueManager
import com.instasaver.app.core.extractor.InstagramExtractor
import com.instasaver.app.core.model.MediaItem
import com.instasaver.app.core.model.MediaType
import com.instasaver.app.ui.components.VideoPlayerView
import com.instasaver.app.ui.theme.AmoledBlack
import com.instasaver.app.ui.theme.DarkSurface
import com.instasaver.app.ui.theme.DarkSurfaceBorder
import com.instasaver.app.ui.theme.DarkSurfaceElevated
import com.instasaver.app.ui.theme.IgOrange
import com.instasaver.app.ui.theme.IgPink
import com.instasaver.app.ui.theme.IgPurple
import com.instasaver.app.ui.theme.InstagramGradient
import com.instasaver.app.ui.theme.SuccessGreen
import com.instasaver.app.ui.theme.TextMuted
import com.instasaver.app.ui.theme.TextPrimary
import com.instasaver.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    sharedUrl: String? = null,
    onExploreProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputUrl by remember { mutableStateOf(sharedUrl ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentMedia by remember { mutableStateOf<MediaItem?>(null) }
    var isPlayingVideo by remember { mutableStateOf(false) }

    // Clipboard Smart Pill state
    var detectedClipboardUrl by remember { mutableStateOf<String?>(null) }

    val downloadStates by DownloadQueueManager.downloadStates.collectAsState()

    // Check clipboard on resume / launch
    LaunchedEffect(Unit) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            if (text.contains("instagram.com/") && text != inputUrl) {
                detectedClipboardUrl = text
            }
        }
    }

    // Auto-fetch if opened via Android Share Sheet
    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            inputUrl = sharedUrl
            fetchUrl(sharedUrl, context, onLoading = { isLoading = it }, onError = { errorMessage = it }, onResult = { currentMedia = it })
        }
    }

    Box(modifier = modifier.fillMaxSize().background(AmoledBlack)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Legal & Disclaimer Banner
            item {
                LegalDisclaimerCard()
            }

            // Input Box with PASTE button
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = InstagramGradient)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = inputUrl,
                                onValueChange = {
                                    inputUrl = it
                                    errorMessage = null
                                },
                                placeholder = {
                                    Text("Paste Instagram Reel or Post link...", color = TextMuted, fontSize = 14.sp)
                                },
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IgPink,
                                    unfocusedBorderColor = DarkSurfaceBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = IgPink
                                ),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    if (inputUrl.isNotEmpty()) {
                                        IconButton(onClick = { inputUrl = ""; currentMedia = null }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // [PASTE] Button
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val text = clip.getItemAt(0).text?.toString() ?: ""
                                        inputUrl = text
                                        detectedClipboardUrl = null
                                        fetchUrl(text, context, onLoading = { isLoading = it }, onError = { errorMessage = it }, onResult = { currentMedia = it })
                                    } else {
                                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("PASTE", color = IgPink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Smart Profile Suggestion Pill
                        val detectedProfileUser = remember(inputUrl) { extractUsernameIfProfile(inputUrl) }
                        if (detectedProfileUser != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkSurfaceElevated,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onExploreProfile(detectedProfileUser) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PersonSearch, contentDescription = null, tint = IgPink, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Profile detected: @$detectedProfileUser • Tap to explore profile & reels",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Fetch / Extract CTA
                        Button(
                            onClick = {
                                if (inputUrl.isNotBlank()) {
                                    fetchUrl(inputUrl, context, onLoading = { isLoading = it }, onError = { errorMessage = it }, onResult = { currentMedia = it })
                                } else {
                                    Toast.makeText(context, "Please paste an Instagram link first", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IgPink),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Fetching Media...", color = Color.White, fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fetch & Download", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Error Message Card
            if (errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D161A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFFF6B6B),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // Media Preview & Download Action Card
            currentMedia?.let { media ->
                item {
                    MediaPreviewCard(
                        media = media,
                        isPlaying = isPlayingVideo,
                        onPlayToggle = { isPlayingVideo = !isPlayingVideo },
                        downloadProgress = downloadStates[media.id]
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        // Smart Clipboard Pill (slides up if link is detected in clipboard)
        AnimatedVisibility(
            visible = detectedClipboardUrl != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        ) {
            detectedClipboardUrl?.let { link ->
                SmartClipboardPill(
                    link = link,
                    enabled = !isLoading,
                    onDownload = {
                        inputUrl = link
                        detectedClipboardUrl = null
                        fetchUrl(link, context, onLoading = { isLoading = it }, onError = { errorMessage = it }, onResult = { currentMedia = it })
                    },
                    onDismiss = { detectedClipboardUrl = null }
                )
            }
        }
    }
}

private fun fetchUrl(
    url: String,
    context: Context,
    onLoading: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    onResult: (MediaItem?) -> Unit
) {
    onLoading(true)
    onError(null)
    onResult(null)

    // Use IO for network work, switch to Main for UI state updates
    kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
    ).launch {
        val result = runCatching { InstagramExtractor.extractMedia(url) }
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            onLoading(false)
            val innerResult = result.getOrElse { ex ->
                Result.failure(ex)
            }
            innerResult.onSuccess { media ->
                onResult(media)
            }.onFailure { ex ->
                onError(ex.message ?: "Failed to extract Instagram link.")
            }
        }
    }
}

@Composable
fun MediaPreviewCard(
    media: MediaItem,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    downloadProgress: com.instasaver.app.core.model.DownloadProgress?
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = InstagramGradient)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author header
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (media.authorAvatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = media.authorAvatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier.size(36.dp).clip(CircleShape).border(1.dp, IgPink, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column {
                    Text(
                        text = "@${media.authorUsername}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = media.type.name,
                        color = IgOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Media Preview or Video Player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                if (isPlaying && media.type == MediaType.REEL) {
                    VideoPlayerView(videoUrl = media.downloadUrl)
                } else {
                    AsyncImage(
                        model = media.thumbnailUrl.ifEmpty { media.downloadUrl },
                        contentDescription = "Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (media.type == MediaType.REEL) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(52.dp)
                                .clickable { onPlayToggle() }
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // If Carousel, show thumbnails row
            if (media.type == MediaType.CAROUSEL && media.carouselItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Carousel Items (${media.carouselItems.size}):", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(media.carouselItems) { slide ->
                        AsyncImage(
                            model = slide.thumbnailUrl,
                            contentDescription = "Slide ${slide.index}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                        )
                    }
                }
            }

            // Caption
            if (media.caption.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = media.caption,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Download Progress Bar
            if (downloadProgress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                if (downloadProgress.isCompleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✓ Saved to Phone Gallery!", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else if (downloadProgress.error != null) {
                    Text("Error: ${downloadProgress.error}", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                } else {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Downloading...", color = TextSecondary, fontSize = 12.sp)
                            if (downloadProgress.progress >= 0f) {
                                Text("${(downloadProgress.progress * 100).toInt()}%", color = IgPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            } else {
                                Text("Saving...", color = IgPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (downloadProgress.progress >= 0f) {
                            LinearProgressIndicator(
                                progress = { downloadProgress.progress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = IgPink,
                                trackColor = DarkSurfaceElevated
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = IgPink,
                                trackColor = DarkSurfaceElevated
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Main Download Button
                Button(
                    onClick = {
                        if (media.type == MediaType.CAROUSEL && media.carouselItems.isNotEmpty()) {
                            val itemsToDownload = media.carouselItems.map { slide ->
                                MediaItem(
                                    id = "${media.shortcode}_slide_${slide.index}",
                                    shortcode = "${media.shortcode}_${slide.index}",
                                    type = slide.type,
                                    url = media.url,
                                    downloadUrl = slide.downloadUrl,
                                    thumbnailUrl = slide.thumbnailUrl,
                                    title = "Slide ${slide.index} by @${media.authorUsername}",
                                    caption = media.caption,
                                    authorUsername = media.authorUsername
                                )
                            }
                            DownloadQueueManager.enqueueBatch(context, itemsToDownload)
                            Toast.makeText(context, "Queued all ${itemsToDownload.size} slides for download!", Toast.LENGTH_SHORT).show()
                        } else {
                            DownloadQueueManager.enqueueDownload(
                                context = context,
                                item = media,
                                onSuccess = { Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show() },
                                onError = { Toast.makeText(context, "Download failed: $it", Toast.LENGTH_LONG).show() }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        when (media.type) {
                            MediaType.REEL -> "Download 1080p Video"
                            MediaType.POST_VIDEO -> "Download Video"
                            MediaType.CAROUSEL -> "Download All Slides (${media.carouselItems.size})"
                            MediaType.AUDIO -> "Download Audio (M4A)"
                            else -> "Download Photo"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Extract Audio Button
                    if (media.audioTrack != null) {
                        Button(
                            onClick = {
                                val audioItem = media.copy(
                                    type = MediaType.AUDIO,
                                    downloadUrl = media.audioTrack.audioUrl
                                )
                                DownloadQueueManager.enqueueDownload(
                                    context = context,
                                    item = audioItem,
                                    onSuccess = { Toast.makeText(context, "Audio Saved to Music!", Toast.LENGTH_SHORT).show() }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
                        ) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = IgOrange, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Audio (M4A)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Copy Caption Button
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Caption", media.caption))
                            Toast.makeText(context, "Caption copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = IgPurple, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Caption", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun SmartClipboardPill(
    link: String,
    enabled: Boolean = true,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(24.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = InstagramGradient),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text("⚡ Reel link detected!", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onDownload,
                enabled = enabled,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IgPink),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Download Now", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Clear, contentDescription = "Dismiss", tint = TextMuted)
            }
        }
    }
}

@Composable
fun LegalDisclaimerCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.Security, contentDescription = "Legal", tint = IgOrange, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Fair-Use Utility: Please respect creator copyrights. Not affiliated with Instagram.",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

private fun extractUsernameIfProfile(input: String): String? {
    val clean = input.trim()
    if (clean.startsWith("@") && clean.length > 1 && !clean.contains(" ")) {
        return clean.removePrefix("@")
    }
    val profilePattern = java.util.regex.Pattern.compile("(?:instagram\\.com|instagr\\.am)/([a-zA-Z0-9._]+)/?$")
    val matcher = profilePattern.matcher(clean.substringBefore("?"))
    if (matcher.find()) {
        val user = matcher.group(1) ?: return null
        val reserved = setOf("p", "reel", "reels", "stories", "tv", "explore", "direct", "accounts", "legal", "about")
        if (user !in reserved) return user
    }
    return null
}

