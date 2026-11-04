package com.instasaver.app.ui.gallery

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.instasaver.app.core.gallery.GalleryScanner
import com.instasaver.app.core.gallery.LocalSavedItem
import com.instasaver.app.ui.components.VideoPlayerView
import com.instasaver.app.ui.theme.AmoledBlack
import com.instasaver.app.ui.theme.DarkSurface
import com.instasaver.app.ui.theme.DarkSurfaceElevated
import com.instasaver.app.ui.theme.IgOrange
import com.instasaver.app.ui.theme.IgPink
import com.instasaver.app.ui.theme.TextMuted
import com.instasaver.app.ui.theme.TextPrimary
import com.instasaver.app.ui.theme.TextSecondary

@Composable
fun GalleryScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<LocalSavedItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All") }
    var playingItem by remember { mutableStateOf<LocalSavedItem?>(null) }

    LaunchedEffect(Unit) {
        items = GalleryScanner.loadSavedMedia(context)
        isLoading = false
    }

    val creators = remember(items) {
        items.map { it.creatorName }.distinct().filter { it.isNotBlank() && it != "General" }
    }

    val filteredItems = remember(items, selectedFilter) {
        when (selectedFilter) {
            "All" -> items
            "Videos" -> items.filter { it.isVideo }
            "Photos" -> items.filter { !it.isVideo }
            else -> items.filter { it.creatorName.equals(selectedFilter, ignoreCase = true) }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(AmoledBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Saved Downloads", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("(${items.size})", color = IgPink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            // Filter Chips (All, Videos, Photos, and Creator names)
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    GalleryChip(name = "All", isSelected = selectedFilter == "All") { selectedFilter = "All" }
                }
                item {
                    GalleryChip(name = "Videos", isSelected = selectedFilter == "Videos") { selectedFilter = "Videos" }
                }
                item {
                    GalleryChip(name = "Photos", isSelected = selectedFilter == "Photos") { selectedFilter = "Photos" }
                }
                items(creators) { creator ->
                    GalleryChip(name = "@$creator", isSelected = selectedFilter == creator) { selectedFilter = creator }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IgPink)
                }
            } else if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = TextMuted, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Downloads Yet", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Downloaded reels and photos will appear here automatically organized by creator.", color = TextSecondary, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filteredItems) { item ->
                        GalleryGridItem(
                            item = item,
                            onClick = {
                                if (item.isVideo) {
                                    playingItem = item
                                }
                            },
                            onShare = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = if (item.isVideo) "video/*" else "image/*"
                                    putExtra(Intent.EXTRA_STREAM, item.uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                            }
                        )
                    }
                }
            }
        }

        // Fullscreen In-App Player Dialog
        playingItem?.let { item ->
            Dialog(onDismissRequest = { playingItem = null }) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    VideoPlayerView(videoUrl = item.uri.toString(), modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun GalleryChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(name, color = if (isSelected) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isSelected) IgPink else DarkSurfaceElevated
        ),
        shape = RoundedCornerShape(16.dp),
        border = null
    )
}

@Composable
fun GalleryGridItem(
    item: LocalSavedItem,
    onClick: () -> Unit,
    onShare: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(DarkSurfaceElevated)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (item.isVideo) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center).size(28.dp)
            )
        }

        // Share icon
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(28.dp)
                .clickable { onShare() }
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = "Share",
                tint = Color.White,
                modifier = Modifier.padding(6.dp)
            )
        }

        // Creator badge
        if (item.creatorName != "General") {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
            ) {
                Text(
                    text = "@${item.creatorName}",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

