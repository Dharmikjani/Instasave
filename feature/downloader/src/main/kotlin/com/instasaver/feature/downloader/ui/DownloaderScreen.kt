package com.instasaver.feature.downloader.ui

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.instasaver.core.model.DownloadProgress
import com.instasaver.core.model.MediaItem
import com.instasaver.core.model.MediaType
import com.instasaver.feature.downloader.ui.components.VideoPlayerView
import com.instasaver.feature.downloader.ui.theme.AmoledBlack
import com.instasaver.feature.downloader.ui.theme.DarkSurface
import com.instasaver.feature.downloader.ui.theme.DarkSurfaceBorder
import com.instasaver.feature.downloader.ui.theme.DarkSurfaceElevated
import com.instasaver.feature.downloader.ui.theme.IgOrange
import com.instasaver.feature.downloader.ui.theme.IgPink
import com.instasaver.feature.downloader.ui.theme.IgPurple
import com.instasaver.feature.downloader.ui.theme.InstagramGradient
import com.instasaver.feature.downloader.ui.theme.SuccessGreen
import com.instasaver.feature.downloader.ui.theme.TextMuted
import com.instasaver.feature.downloader.ui.theme.TextPrimary
import com.instasaver.feature.downloader.ui.theme.TextSecondary
import java.util.regex.Pattern

@Composable
fun DownloaderScreen(
    sharedUrl: String? = null,
    onExploreProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DownloaderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var inputUrl by remember { mutableStateOf(sharedUrl ?: "") }
    var detectedClipboardUrl by remember { mutableStateOf<String?>(null) }
    var isPlayingVideo by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val checkClipboard = {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).text?.toString()?.trim() ?: ""
                    if (text.contains("instagram.com/") && text != inputUrl) {
                        detectedClipboardUrl = text
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        checkClipboard()

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkClipboard()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            inputUrl = sharedUrl
            viewModel.onIntent(DownloaderUiIntent.FetchUrl(sharedUrl))
        }
    }

    val isLoading = uiState is DownloaderUiState.Loading
    val currentMedia = (uiState as? DownloaderUiState.Success)?.mediaItem
    val errorMessage = (uiState as? DownloaderUiState.Error)?.message

    Box(modifier = modifier.fillMaxSize().background(AmoledBlack)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item { LegalDisclaimerCard() }

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
                                onValueChange = { inputUrl = it },
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
                                        IconButton(onClick = {
                                            inputUrl = ""
                                            viewModel.onIntent(DownloaderUiIntent.Clear)
                                        }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            val text = clip.getItemAt(0).text?.toString() ?: ""
                                            inputUrl = text
                                            detectedClipboardUrl = null
                                            viewModel.onIntent(DownloaderUiIntent.FetchUrl(text))
                                        } else {
                                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("PASTE", color = IgPink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

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

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (inputUrl.isNotBlank()) {
                                    viewModel.onIntent(DownloaderUiIntent.FetchUrl(inputUrl))
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
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fetching Media...", color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fetch Media", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1619)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFF8585),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    com.instasaver.feature.downloader.ui.components.DownloaderShimmerSkeleton()
                }
            }

            if (currentMedia != null && !isLoading) {
                item {
                    MediaPreviewCard(
                        media = currentMedia,
                        isPlaying = isPlayingVideo,
                        downloadProgress = downloadStates[currentMedia.id],
                        onPlayToggle = { isPlayingVideo = !isPlayingVideo },
                        onDownloadMedia = { item, url -> viewModel.onIntent(DownloaderUiIntent.DownloadMedia(item, url)) },
                        onDownloadAudio = { item -> viewModel.onIntent(DownloaderUiIntent.DownloadAudio(item)) },
                        onExploreProfile = onExploreProfile
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }

        AnimatedVisibility(
            visible = detectedClipboardUrl != null && !isLoading,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
        ) {
            SmartClipboardPill(
                link = detectedClipboardUrl ?: "",
                onDownload = {
                    val url = detectedClipboardUrl ?: ""
                    inputUrl = url
                    detectedClipboardUrl = null
                    viewModel.onIntent(DownloaderUiIntent.FetchUrl(url))
                },
                onDismiss = { detectedClipboardUrl = null }
            )
        }
    }
}

@Composable
fun MediaPreviewCard(
    media: MediaItem,
    isPlaying: Boolean,
    downloadProgress: DownloadProgress?,
    onPlayToggle: () -> Unit,
    onDownloadMedia: (MediaItem, String?) -> Unit,
    onDownloadAudio: (MediaItem) -> Unit,
    onExploreProfile: (String) -> Unit
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (media.authorAvatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = media.authorAvatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, IgPink, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "@${media.authorUsername}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = media.type.name.replace("_", " "),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                if (media.authorUsername.isNotEmpty() && media.authorUsername != "instagram_user") {
                    Button(
                        onClick = { onExploreProfile(media.authorUsername) },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Explore", color = IgPink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Media Preview or ExoPlayer
            if (isPlaying && (media.type == MediaType.REEL || media.type == MediaType.POST_VIDEO)) {
                VideoPlayerView(
                    videoUrl = media.downloadUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                ) {
                    AsyncImage(
                        model = media.thumbnailUrl.ifEmpty { media.downloadUrl },
                        contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (media.type == MediaType.REEL || media.type == MediaType.POST_VIDEO) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(56.dp)
                                .clickable { onPlayToggle() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                }
            }

            // Carousel Slides Horizontal List
            if (media.type == MediaType.CAROUSEL && media.carouselItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Carousel Slides (${media.carouselItems.size}):", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(media.carouselItems) { slide ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.width(100.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(6.dp)) {
                                AsyncImage(
                                    model = slide.thumbnailUrl.ifEmpty { slide.downloadUrl },
                                    contentDescription = "Slide ${slide.index}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(88.dp).clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        val singleSlideItem = media.copy(
                                            id = "${media.shortcode}_slide_${slide.index}",
                                            shortcode = "${media.shortcode}_${slide.index}",
                                            type = slide.type,
                                            downloadUrl = slide.downloadUrl,
                                            title = "Slide ${slide.index} by @${media.authorUsername}"
                                        )
                                        onDownloadMedia(singleSlideItem, slide.downloadUrl)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = IgPink),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("#${slide.index}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (media.caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = media.caption,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Download Progress Bar
            if (downloadProgress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                if (downloadProgress.isCompleted) {
                    Text("✓ Saved to Phone Gallery!", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                } else if (downloadProgress.error != null) {
                    Text("Error: ${downloadProgress.error}", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                } else {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Downloading...", color = TextSecondary, fontSize = 12.sp)
                            val pct = if (downloadProgress.progress >= 0f) "${(downloadProgress.progress * 100).toInt()}%" else "Saving..."
                            Text(pct, color = IgPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { if (downloadProgress.progress >= 0f) downloadProgress.progress else 0.5f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = IgPink,
                            trackColor = DarkSurfaceElevated
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (media.type == MediaType.CAROUSEL && media.carouselItems.isNotEmpty()) {
                            media.carouselItems.forEach { slide ->
                                val item = media.copy(
                                    id = "${media.shortcode}_slide_${slide.index}",
                                    shortcode = "${media.shortcode}_${slide.index}",
                                    type = slide.type,
                                    downloadUrl = slide.downloadUrl,
                                    title = "Slide ${slide.index} by @${media.authorUsername}"
                                )
                                onDownloadMedia(item, slide.downloadUrl)
                            }
                            Toast.makeText(context, "Queued all ${media.carouselItems.size} slides!", Toast.LENGTH_SHORT).show()
                        } else {
                            onDownloadMedia(media, media.downloadUrl)
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
                    if (media.thumbnailUrl.isNotBlank()) {
                        Button(
                            onClick = {
                                val coverItem = media.copy(
                                    id = "${media.shortcode}_cover",
                                    type = MediaType.POST_IMAGE,
                                    downloadUrl = media.thumbnailUrl,
                                    title = "Cover - ${media.authorUsername}"
                                )
                                onDownloadMedia(coverItem, media.thumbnailUrl)
                                Toast.makeText(context, "Saving Cover Image...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = IgPink, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Cover", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (media.audioTrack != null || media.type == MediaType.REEL || media.type == MediaType.POST_VIDEO) {
                        Button(
                            onClick = { onDownloadAudio(media) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
                        ) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = IgOrange, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Audio (M4A)", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

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
                        Text("Copy Caption", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
    val profilePattern = Pattern.compile("(?:instagram\\.com|instagr\\.am)/([a-zA-Z0-9._]+)/?$")
    val matcher = profilePattern.matcher(clean.substringBefore("?"))
    if (matcher.find()) {
        val user = matcher.group(1) ?: return null
        val reserved = setOf("p", "reel", "reels", "stories", "tv", "explore", "direct", "accounts", "legal", "about")
        if (user !in reserved) return user
    }
    return null
}
