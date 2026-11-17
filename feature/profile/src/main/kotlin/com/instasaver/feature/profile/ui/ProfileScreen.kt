package com.instasaver.feature.profile.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import com.instasaver.feature.profile.ui.components.VideoPlayerView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.instasaver.core.model.MediaItem
import com.instasaver.core.model.MediaType
import com.instasaver.core.model.ProfileData
import com.instasaver.feature.profile.ui.theme.AmoledBlack
import com.instasaver.feature.profile.ui.theme.DarkSurface
import com.instasaver.feature.profile.ui.theme.DarkSurfaceBorder
import com.instasaver.feature.profile.ui.theme.DarkSurfaceElevated
import com.instasaver.feature.profile.ui.theme.IgPink
import com.instasaver.feature.profile.ui.theme.InfoBlue
import com.instasaver.feature.profile.ui.theme.StoryRingGradient
import com.instasaver.feature.profile.ui.theme.SuccessGreen
import com.instasaver.feature.profile.ui.theme.TextMuted
import com.instasaver.feature.profile.ui.theme.TextPrimary
import com.instasaver.feature.profile.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileExplorerScreen(
    initialUsername: String? = null,
    onUsernameConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var searchInput by remember { mutableStateOf(initialUsername ?: "") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDpZoomDialog by remember { mutableStateOf(false) }
    var showDownloadAllSheet by remember { mutableStateOf(false) }
    var selectedPreviewMedia by remember { mutableStateOf<MediaItem?>(null) }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(initialUsername) {
        if (!initialUsername.isNullOrBlank()) {
            searchInput = initialUsername
            viewModel.onIntent(ProfileUiIntent.Search(initialUsername))
            onUsernameConsumed()
        }
    }

    val isLoading = uiState is ProfileUiState.Loading
    val profileData = (uiState as? ProfileUiState.Success)?.profile
    val isPinned = (uiState as? ProfileUiState.Success)?.isPinned ?: false
    val errorMessage = (uiState as? ProfileUiState.Error)?.message

    Box(modifier = modifier.fillMaxSize().background(AmoledBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        placeholder = {
                            Text("Search @username without login...", color = TextMuted, fontSize = 14.sp)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IgPink,
                            unfocusedBorderColor = DarkSurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = IgPink
                        ),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = IgPink)
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { viewModel.onIntent(ProfileUiIntent.Search(searchInput)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IgPink),
                        enabled = !isLoading && searchInput.isNotBlank()
                    ) {
                        Text("Search", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = IgPink)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Exploring Profile in Ghost Mode...", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }

            if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = errorMessage,
                                color = Color(0xFFFF6B6B),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (searchInput.isNotBlank()) {
                                        viewModel.onIntent(ProfileUiIntent.Search(searchInput))
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IgPink)
                            ) {
                                Text("Retry Search", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            profileData?.let { profile ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .border(2.5.dp, StoryRingGradient, CircleShape)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .clickable { showDpZoomDialog = true }
                            ) {
                                AsyncImage(
                                    model = profile.hdAvatarUrl,
                                    contentDescription = "Profile Pic",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                StatColumn(title = "Posts", value = profile.postsCount.toString())
                                StatColumn(title = "Followers", value = profile.followersCount.ifEmpty { "--" })
                                StatColumn(title = "Following", value = profile.followingCount.ifEmpty { "--" })
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.fullName.ifEmpty { "@${profile.username}" },
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (profile.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Verified, contentDescription = "Verified", tint = InfoBlue, modifier = Modifier.size(16.dp))
                            }
                        }

                        Text(text = "@${profile.username}", color = IgPink, fontSize = 13.sp, fontWeight = FontWeight.Medium)

                        if (profile.bio.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = profile.bio,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (profile.externalUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    try {
                                        val rawUrl = profile.externalUrl.trim()
                                        val targetUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                                            "https://$rawUrl"
                                        } else {
                                            rawUrl
                                        }
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                        context.startActivity(browserIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Link, contentDescription = "Website", tint = InfoBlue, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = profile.externalUrl,
                                    color = InfoBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.onIntent(ProfileUiIntent.TogglePin(profile.username)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPinned) IgPink else DarkSurface
                                )
                            ) {
                                Icon(
                                    imageVector = if (isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPinned) "Pinned" else "+ Watchlist",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val allMedia = profile.reels + profile.posts
                            if (allMedia.isNotEmpty()) {
                                Button(
                                    onClick = { showDownloadAllSheet = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download All", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = IgPink,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = IgPink
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Posts (${profile.posts.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Reels (${profile.reels.size})", fontWeight = FontWeight.Bold) }
                    )
                }

                val mediaList = if (selectedTab == 0) profile.posts else profile.reels
                if (mediaList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when {
                                    profile.isPrivate -> "This account is private."
                                    profile.postsCount == 0 && profile.followersCount.isEmpty() -> "No public media available in Guest Mode."
                                    else -> "No media available in this section."
                                },
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            if (!profile.isPrivate && profile.postsCount == 0 && profile.followersCount.isEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Connect Instagram via 'Login (Unlock All)' in the top bar to view protected or private profiles with 100% reliability.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize().padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(mediaList) { item ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clickable {
                                        selectedPreviewMedia = item
                                    }
                            ) {
                                AsyncImage(
                                    model = item.thumbnailUrl.ifEmpty { item.downloadUrl },
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (item.type == MediaType.REEL || item.type == MediaType.POST_VIDEO) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(16.dp)
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .size(28.dp)
                                        .clickable {
                                            viewModel.onIntent(ProfileUiIntent.DownloadItem(item))
                                            Toast.makeText(context, "Queued for download!", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = "Quick Download",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        selectedPreviewMedia?.let { media ->
            ProfileMediaPreviewDialog(
                media = media,
                onDismiss = { selectedPreviewMedia = null },
                onDownload = {
                    viewModel.onIntent(ProfileUiIntent.DownloadItem(media))
                    Toast.makeText(context, "Downloading to Gallery...", Toast.LENGTH_SHORT).show()
                    selectedPreviewMedia = null
                },
                onDownloadAudio = {
                    viewModel.onIntent(ProfileUiIntent.DownloadAudio(media))
                    Toast.makeText(context, "Extracting Audio (M4A)...", Toast.LENGTH_SHORT).show()
                    selectedPreviewMedia = null
                },
                onShare = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, media.url)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Media Link"))
                }
            )
        }

        if (showDpZoomDialog && profileData != null) {
            HdDpDialog(
                profile = profileData,
                onDismiss = { showDpZoomDialog = false },
                onDownload = {
                    val dpItem = MediaItem(
                        id = "${profileData.username}_dp",
                        shortcode = "dp",
                        type = MediaType.PROFILE_DP,
                        url = profileData.hdAvatarUrl,
                        downloadUrl = profileData.hdAvatarUrl,
                        thumbnailUrl = profileData.hdAvatarUrl,
                        authorUsername = profileData.username,
                        title = "DP of @${profileData.username}"
                    )
                    viewModel.onIntent(ProfileUiIntent.DownloadItem(dpItem))
                    Toast.makeText(context, "Saved HD Avatar to Gallery!", Toast.LENGTH_SHORT).show()
                    showDpZoomDialog = false
                }
            )
        }

        if (showDownloadAllSheet && profileData != null) {
            val reelsList = profileData.reels.filter { it.type == MediaType.REEL || it.type == MediaType.POST_VIDEO }
            val postsList = profileData.posts.filter { it.type == MediaType.POST_IMAGE }
            val carouselsList = (profileData.posts + profileData.reels).filter { it.type == MediaType.CAROUSEL }
            val allMedia = profileData.reels + profileData.posts

            ModalBottomSheet(
                onDismissRequest = { showDownloadAllSheet = false },
                containerColor = DarkSurfaceElevated
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Bulk Download Media", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Select which media category to download from @${profileData.username}:", color = TextSecondary, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(4.dp))

                    // Option 1: Download All Reels
                    Button(
                        onClick = {
                            if (reelsList.isNotEmpty()) {
                                viewModel.onIntent(ProfileUiIntent.DownloadBatch(reelsList))
                                Toast.makeText(context, "Queued ${reelsList.size} Reels for download!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No reels found for this creator", Toast.LENGTH_SHORT).show()
                            }
                            showDownloadAllSheet = false
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IgPink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🎬 Download All Reels (${reelsList.size} items)", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Option 2: Download All Posts (Photos)
                    Button(
                        onClick = {
                            if (postsList.isNotEmpty()) {
                                viewModel.onIntent(ProfileUiIntent.DownloadBatch(postsList))
                                Toast.makeText(context, "Queued ${postsList.size} Posts for download!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No photo posts found for this creator", Toast.LENGTH_SHORT).show()
                            }
                            showDownloadAllSheet = false
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("📸 Download All Posts (${postsList.size} items)", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Option 3: Download All Carousels
                    Button(
                        onClick = {
                            if (carouselsList.isNotEmpty()) {
                                viewModel.onIntent(ProfileUiIntent.DownloadBatch(carouselsList))
                                Toast.makeText(context, "Queued ${carouselsList.size} Carousels for download!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No carousels found for this creator", Toast.LENGTH_SHORT).show()
                            }
                            showDownloadAllSheet = false
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("📑 Download All Carousels (${carouselsList.size} items)", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Option 4: Download Everything
                    if (allMedia.isNotEmpty()) {
                        Button(
                            onClick = {
                                viewModel.onIntent(ProfileUiIntent.DownloadBatch(allMedia))
                                Toast.makeText(context, "Queued all ${allMedia.size} items for download!", Toast.LENGTH_SHORT).show()
                                showDownloadAllSheet = false
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("⚡ Download Everything (${allMedia.size} items)", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun StatColumn(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(text = title, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
fun HdDpDialog(
    profile: ProfileData,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("HD Profile Picture", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("@${profile.username}", color = IgPink, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Pinch to zoom • Drag to pan", color = TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    val maxOffset = (scale - 1f) * 300f
                                    offsetX = (offsetX + pan.x).coerceIn(-maxOffset, maxOffset)
                                    offsetY = (offsetY + pan.y).coerceIn(-maxOffset, maxOffset)
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                ) {
                    AsyncImage(
                        model = profile.hdAvatarUrl,
                        contentDescription = "HD DP",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            },
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save HD Avatar to Gallery", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProfileMediaPreviewDialog(
    media: MediaItem,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onDownloadAudio: () -> Unit,
    onShare: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val isVideo = media.type == MediaType.REEL || media.type == MediaType.POST_VIDEO

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isVideo) "Reel Preview" else "Photo Preview",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text("@${media.authorUsername}", color = IgPink, fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                ) {
                    if (isVideo) {
                        VideoPlayerView(
                            videoUrl = media.downloadUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = media.downloadUrl.ifEmpty { media.thumbnailUrl },
                            contentDescription = "Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 5f)
                                        if (scale > 1f) {
                                            val maxOffset = (scale - 1f) * 300f
                                            offsetX = (offsetX + pan.x).coerceIn(-maxOffset, maxOffset)
                                            offsetY = (offsetY + pan.y).coerceIn(-maxOffset, maxOffset)
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    }
                                }
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offsetX
                                    translationY = offsetY
                                }
                        )
                    }
                }

                if (media.caption.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = media.caption,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isVideo) "Download Video (1080p)" else "Download Photo",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isVideo) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDownloadAudio,
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
                        ) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = IgPink, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Audio (M4A)", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = onShare,
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
