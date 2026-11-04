package com.instasaver.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.instasaver.app.core.watchlist.WatchlistManager
import com.instasaver.app.ui.gallery.GalleryScreen
import com.instasaver.app.ui.home.HomeScreen
import com.instasaver.app.ui.profile.ProfileExplorerScreen
import com.instasaver.app.ui.theme.AmoledBlack
import com.instasaver.app.ui.theme.DarkSurface
import com.instasaver.app.ui.theme.IgPink
import com.instasaver.app.ui.theme.InstaSaverTheme
import com.instasaver.app.ui.theme.TextMuted
import com.instasaver.app.ui.theme.TextPrimary
import com.instasaver.app.ui.watchlist.WatchlistScreen

class MainActivity : ComponentActivity() {

    private val sharedInstagramUrl = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WatchlistManager.init(this)
        com.instasaver.app.core.network.InstagramSessionManager.init(this)

        handleIncomingIntent(intent)
        enableEdgeToEdge()

        setContent {
            InstaSaverTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val sharedUrl by sharedInstagramUrl.collectAsState()
                    AppRootContainer(initialSharedUrl = sharedUrl)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            if (text.contains("instagram.com/")) {
                sharedInstagramUrl.value = text
            }
        }
    }
}

@Composable
fun AppRootContainer(initialSharedUrl: String?) {
    var selectedTab by remember { mutableIntStateOf(if (initialSharedUrl != null) 0 else 0) }
    var explorerTargetUsername by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(initialSharedUrl) {
        if (!initialSharedUrl.isNullOrBlank()) {
            selectedTab = 0
        }
    }

    val isLoggedIn by com.instasaver.app.core.network.InstagramSessionManager.isLoggedIn.collectAsState()
    var showLoginDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AmoledBlack)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "InstaSaver Pro",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )

                AssistChip(
                    onClick = {
                        if (isLoggedIn) {
                            com.instasaver.app.core.network.InstagramSessionManager.logout()
                        } else {
                            showLoginDialog = true
                        }
                    },
                    label = {
                        Text(
                            text = if (isLoggedIn) "🔐 Connected" else "🔓 Login (Unlock All)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLoggedIn) com.instasaver.app.ui.theme.SuccessGreen else IgPink
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = DarkSurface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = null
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Download, contentDescription = "Home") },
                    label = { Text("Downloader", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IgPink,
                        selectedTextColor = IgPink,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = DarkSurface
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.PersonSearch, contentDescription = "Profile") },
                    label = { Text("Profile", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IgPink,
                        selectedTextColor = IgPink,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = DarkSurface
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = "Watchlist") },
                    label = { Text("Watchlist", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IgPink,
                        selectedTextColor = IgPink,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = DarkSurface
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Gallery") },
                    label = { Text("Gallery", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IgPink,
                        selectedTextColor = IgPink,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = DarkSurface
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    sharedUrl = initialSharedUrl,
                    onExploreProfile = { username ->
                        explorerTargetUsername = username
                        selectedTab = 1 // Switch to Profile Explorer
                    }
                )
                1 -> ProfileExplorerScreen(initialUsername = explorerTargetUsername)
                2 -> WatchlistScreen(onCreatorClick = { username ->
                    explorerTargetUsername = username
                    selectedTab = 1 // Switch to Profile Explorer
                })
                3 -> GalleryScreen()
            }
        }

        if (showLoginDialog) {
            com.instasaver.app.ui.components.InstagramLoginDialog(
                onDismiss = { showLoginDialog = false },
                onLoginSuccess = { showLoginDialog = false }
            )
        }
    }
}
