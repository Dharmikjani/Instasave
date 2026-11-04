package com.instasaver.app.ui.profile

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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.instasaver.app.core.download.DownloadQueueManager
import com.instasaver.app.core.extractor.ProfileScraper
import com.instasaver.app.core.model.MediaItem
import com.instasaver.app.core.model.MediaType
import com.instasaver.app.core.model.ProfileData
import com.instasaver.app.core.watchlist.WatchlistManager
import com.instasaver.app.ui.components.VideoPlayerView
import com.instasaver.app.ui.theme.AmoledBlack
import com.instasaver.app.ui.theme.DarkSurface
import com.instasaver.app.ui.theme.DarkSurfaceBorder
import com.instasaver.app.ui.theme.DarkSurfaceElevated
import com.instasaver.app.ui.theme.IgOrange
import com.instasaver.app.ui.theme.IgPink
import com.instasaver.app.ui.theme.InfoBlue
import com.instasaver.app.ui.theme.InstagramGradient
import com.instasaver.app.ui.theme.StoryRingGradient
import com.instasaver.app.ui.theme.SuccessGreen
import com.instasaver.app.ui.theme.TextMuted
import com.instasaver.app.ui.theme.TextPrimary
import com.instasaver.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileExplorerScreen(
    initialUsername: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchInput by remember { mutableStateOf(initialUsername ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var profileData by remember { mutableStateOf<ProfileData?>(null) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showDpZoomDialog by remember { mutableStateOf(false) }
    var showDownloadAllSheet by remember { mutableStateOf(false) }
    var playingMedia by remember { mutableStateOf<MediaItem?>(null) }

    val pinnedCreators by WatchlistManager.creators.collectAsState()
    val isLoggedIn by com.instasaver.app.core.network.InstagramSessionManager.isLoggedIn.collectAsState()
    var showLoginDialog by remember { mutableStateOf(false) }

    fun doSearch(username: String) {
        if (username.isBlank()) return
        isLoading = true
        errorMessage = null
        profileData = null

        scope.launch {
            val result = ProfileScraper.getProfile(username)
            isLoading = false
            result.onSuccess { data ->
                profileData = data
            }.onFailure { ex ->
                errorMessage = ex.message ?: "Could not find profile."
            }
        }
    }

    // Auto-search if initialUsername provided
    androidx.compose.runtime.LaunchedEffect(initialUsername) {
        if (!initialUsername.isNullOrBlank()) {
            searchInput = initialUsername
            doSearch(initialUsername)
        }
    }

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
                        onClick = { doSearch(searchInput) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IgPink),
                        enabled = !isLoading && searchInput.isNotBlank()
                    ) {
                        Text("Search", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Loading state
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = IgPink)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Exploring Profile in Ghost Mode...", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }

            // Error state
            if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage ?: "", color = Color(0xFFFF6B6B), fontSize = 14.sp)
                }
            }

            // Loaded Profile Content
            profileData?.let { profile ->
                // Profile Header Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // HD DP with story ring
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

                            // Stats: Posts, Followers, Following
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

                        // Full Name and Username
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // Actions Row: Pin Watchlist + "Download All"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isPinned = pinnedCreators.contains(profile.username)
                            Button(
                                onClick = {
                                    val added = WatchlistManager.toggleCreator(profile.username)
                                    Toast.makeText(context, if (added) "Pinned to Watchlist!" else "Removed from Watchlist", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPinned) DarkSurface else IgPink
                                )
                            ) {
                                Icon(
                                    if (isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPinned) "Pinned" else "+ Watchlist", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            // "Download All" Bulk Button
                            Button(
                                onClick = { showDownloadAllSheet = true },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download All", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // Tabs: Reels / Posts
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = IgPink,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = IgPink
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Reels (${profile.reels.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Posts (${profile.posts.size})", fontWeight = FontWeight.Bold) }
                    )
                }

                // Grid Feed
                val currentItems = if (selectedTab == 0) profile.reels else profile.posts
                if (currentItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (profile.isPrivate) "This account is private." else "No ${if (selectedTab == 0) "reels" else "posts"} loaded yet.",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (profile.isPrivate)
                                    "To view reels and posts from private accounts, follow them on Instagram first."
                                else if (!isLoggedIn)
                                    "Instagram restricts loading reels without login on mobile networks. Connect your Instagram account to unlock all reels!"
                                else
                                    "Tap reload below to fetch the latest reels and posts.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            if (!isLoggedIn) {
                                Button(
                                    onClick = { showLoginDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = IgPink),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connect Instagram Account", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            } else {
                                Button(
                                    onClick = { doSearch(profile.username) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reload Profile", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize().padding(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(currentItems) { media ->
                            ProfileMediaGridItem(
                                media = media,
                                onPlay = { playingMedia = media },
                                onDownload = {
                                    DownloadQueueManager.enqueueDownload(
                                        context = context,
                                        item = media,
                                        onSuccess = { Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show() }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Full HD DP Zoom Dialog
        if (showDpZoomDialog && profileData != null) {
            Dialog(onDismissRequest = { showDpZoomDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("@${profileData!!.username} • Full HD DP", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        AsyncImage(
                            model = profileData!!.hdAvatarUrl,
                            contentDescription = "HD DP",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val dpMedia = MediaItem(
                                    id = "${profileData!!.username}_dp",
                                    shortcode = "dp",
                                    type = MediaType.PROFILE_DP,
                                    url = profileData!!.hdAvatarUrl,
                                    downloadUrl = profileData!!.hdAvatarUrl,
                                    thumbnailUrl = profileData!!.hdAvatarUrl,
                                    authorUsername = profileData!!.username,
                                    title = "${profileData!!.username} HD Profile Picture"
                                )
                                DownloadQueueManager.enqueueDownload(context, dpMedia) {
                                    Toast.makeText(context, "HD DP Saved to Gallery!", Toast.LENGTH_SHORT).show()
                                }
                                showDpZoomDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save HD DP to Gallery", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // "Download All" Bulk Category Selector Bottom Sheet
        if (showDownloadAllSheet && profileData != null) {
            ModalBottomSheet(
                onDismissRequest = { showDownloadAllSheet = false },
                containerColor = DarkSurfaceElevated
            ) {
                Column(modifier = Modifier.padding(20.dp).padding(bottom = 24.dp)) {
                    Text("Bulk Download for @${profileData!!.username}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select a category to batch download to your phone gallery:", color = TextSecondary, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Download All Reels
                    if (profileData!!.reels.isNotEmpty()) {
                        Button(
                            onClick = {
                                DownloadQueueManager.enqueueBatch(context, profileData!!.reels)
                                Toast.makeText(context, "Queued all ${profileData!!.reels.size} Reels for download!", Toast.LENGTH_SHORT).show()
                                showDownloadAllSheet = false
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IgPink)
                        ) {
                            Text("Download All Reels (${profileData!!.reels.size} items)", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Download All Posts
                    if (profileData!!.posts.isNotEmpty()) {
                        Button(
                            onClick = {
                                DownloadQueueManager.enqueueBatch(context, profileData!!.posts)
                                Toast.makeText(context, "Queued all ${profileData!!.posts.size} Posts for download!", Toast.LENGTH_SHORT).show()
                                showDownloadAllSheet = false
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IgOrange)
                        ) {
                            Text("Download All Posts (${profileData!!.posts.size} items)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Fullscreen In-App Player Modal
        playingMedia?.let { media ->
            Dialog(onDismissRequest = { playingMedia = null }) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    VideoPlayerView(videoUrl = media.downloadUrl, modifier = Modifier.fillMaxSize())
                }
            }
        }

        // Instagram Login Dialog
        if (showLoginDialog) {
            com.instasaver.app.ui.components.InstagramLoginDialog(
                onDismiss = { showLoginDialog = false },
                onLoginSuccess = {
                    showLoginDialog = false
                    Toast.makeText(context, "Instagram Connected! Reloading profile...", Toast.LENGTH_SHORT).show()
                    doSearch(searchInput)
                }
            )
        }
    }
}

@Composable
fun ProfileMediaGridItem(
    media: MediaItem,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(DarkSurfaceElevated)
            .clickable { onPlay() }
    ) {
        AsyncImage(
            model = media.thumbnailUrl.ifEmpty { media.downloadUrl },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Type Badge (Reel icon)
        if (media.type == MediaType.REEL) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp)
            )
        }

        // 1-Tap Download Button Overlay
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(32.dp)
                .clickable { onDownload() }
        ) {
            Icon(
                Icons.Default.Download,
                contentDescription = "Download",
                tint = Color.White,
                modifier = Modifier.padding(6.dp)
            )
        }
    }
}

@Composable
fun StatColumn(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(text = title, color = TextSecondary, fontSize = 12.sp)
    }
}

