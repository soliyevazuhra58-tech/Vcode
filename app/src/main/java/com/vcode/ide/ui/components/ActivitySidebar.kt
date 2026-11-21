package com.vcode.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vcode.ide.domain.model.SidebarTab
import com.vcode.ide.ui.theme.LocalVCodeColors

@Composable
fun ActivitySidebar(
    activeTab: SidebarTab,
    isSidebarVisible: Boolean,
    isPreviewVisible: Boolean,
    isTerminalVisible: Boolean,
    gitChangeCount: Int,
    onSelectTab: (SidebarTab) -> Unit,
    onToggleTerminal: () -> Unit,
    onOpenCommandPalette: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVCodeColors.current
    Column(
        modifier = modifier
            .width(48.dp)
            .fillMaxHeight()
            .background(colors.activityBarBg)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ActivityBarIcon(Icons.Default.FolderCopy, "Explorer", isSidebarVisible && activeTab == SidebarTab.EXPLORER) { onSelectTab(SidebarTab.EXPLORER) }
        ActivityBarIcon(Icons.Default.Search, "Search", isSidebarVisible && activeTab == SidebarTab.SEARCH) { onSelectTab(SidebarTab.SEARCH) }
        Box(contentAlignment = Alignment.TopEnd) {
            ActivityBarIcon(Icons.Default.AccountTree, "Source Control", isSidebarVisible && activeTab == SidebarTab.SOURCE_CONTROL) { onSelectTab(SidebarTab.SOURCE_CONTROL) }
            if (gitChangeCount > 0) {
                Box(
                    modifier = Modifier.padding(top = 6.dp, end = 6.dp).size(16.dp).clip(CircleShape).background(colors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(gitChangeCount.coerceAtMost(99).toString(), color = colors.statusBarFg, fontSize = 9.sp)
                }
            }
        }
        ActivityBarIcon(Icons.Default.Extension, "Extensions", isSidebarVisible && activeTab == SidebarTab.EXTENSIONS) { onSelectTab(SidebarTab.EXTENSIONS) }
        ActivityBarIcon(Icons.Default.Web, "Live Preview", isPreviewVisible) { onSelectTab(SidebarTab.PREVIEW) }
        ActivityBarIcon(Icons.Default.Terminal, "Terminal", isTerminalVisible, onToggleTerminal)
        Spacer(modifier = Modifier.weight(1f))
        ActivityBarIcon(Icons.Default.PlayArrow, "Command Palette", false, onOpenCommandPalette)
        ActivityBarIcon(Icons.Default.Settings, "Settings", isSidebarVisible && activeTab == SidebarTab.SETTINGS) { onSelectTab(SidebarTab.SETTINGS) }
    }
}

@Composable
private fun ActivityBarIcon(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalVCodeColors.current
    Box(
        modifier = Modifier.fillMaxWidth().height(44.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(modifier = Modifier.align(Alignment.CenterStart).width(2.5.dp).height(26.dp).background(colors.activityBarIconActive))
        }
        Icon(icon, label, tint = if (selected) colors.activityBarIconActive else colors.activityBarIconInactive, modifier = Modifier.size(22.dp))
    }
}
