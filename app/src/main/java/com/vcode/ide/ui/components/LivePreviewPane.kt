package com.vcode.ide.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.vcode.ide.domain.model.ConsoleLogEntry
import com.vcode.ide.domain.model.ViewportConfig
import com.vcode.ide.domain.model.ViewportDeviceType
import com.vcode.ide.ui.theme.LocalVCodeColors
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.net.URLConnection

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LivePreviewPane(
    projectRootPath: String?,
    entryHtmlPath: String?,
    refreshToken: Long,
    viewportConfig: ViewportConfig,
    consoleLogs: List<ConsoleLogEntry>,
    onRefresh: () -> Unit,
    onClose: () -> Unit,
    onSelectDeviceType: (ViewportDeviceType) -> Unit,
    onUpdateCustomSize: (Int, Int) -> Unit,
    onToggleOrientation: () -> Unit,
    onConsoleMessage: (level: String, msg: String, source: String) -> Unit,
    onClearConsole: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVCodeColors.current
    val density = LocalDensity.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showConsole by remember { mutableStateOf(true) }
    var customWidthText by remember(viewportConfig.widthPx) { mutableStateOf(viewportConfig.widthPx.toString()) }
    var customHeightText by remember(viewportConfig.heightPx) { mutableStateOf(viewportConfig.heightPx.toString()) }
    var lastErrorBanner by remember { mutableStateOf<String?>(null) }

    val baseVirtualUrl = "https://vcode.local/"
    val targetRelative = remember(projectRootPath, entryHtmlPath) {
        if (projectRootPath != null && entryHtmlPath != null) {
            runCatching { File(entryHtmlPath).relativeTo(File(projectRootPath)).path.replace(File.separatorChar, '/') }.getOrDefault("index.html")
        } else "index.html"
    }

    LaunchedEffect(refreshToken, targetRelative, projectRootPath) {
        lastErrorBanner = null
        webViewRef?.loadUrl(baseVirtualUrl + targetRelative)
    }

    Column(modifier = modifier.fillMaxSize().background(colors.panelBg).border(0.5.dp, colors.border)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(36.dp).background(colors.sideBarHeaderBg).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = { webViewRef?.goBack() }, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.ArrowBack, "Back", tint = colors.textSecondary, modifier = Modifier.size(15.dp)) }
            IconButton(onClick = { webViewRef?.goForward() }, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.ArrowForward, "Forward", tint = colors.textSecondary, modifier = Modifier.size(15.dp)) }
            IconButton(onClick = onRefresh, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.Refresh, "Refresh", tint = colors.textSecondary, modifier = Modifier.size(15.dp)) }
            Box(modifier = Modifier.weight(1f).background(colors.editorBg, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("vcode.local/$targetRelative", color = colors.textPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { showConsole = !showConsole }, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.Terminal, "Console", tint = if (showConsole) colors.accent else colors.textMuted, modifier = Modifier.size(15.dp))
            }
            IconButton(onClick = onClose, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.Close, "Close", tint = colors.textMuted, modifier = Modifier.size(15.dp)) }
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(colors.sideBarBg).border(0.5.dp, colors.border).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ViewportPresetChip("Fit", null, viewportConfig.deviceType == ViewportDeviceType.RESPONSIVE) { onSelectDeviceType(ViewportDeviceType.RESPONSIVE) }
            ViewportPresetChip("Mobile", Icons.Default.PhoneAndroid, viewportConfig.deviceType == ViewportDeviceType.MOBILE) { onSelectDeviceType(ViewportDeviceType.MOBILE) }
            ViewportPresetChip("Tablet", Icons.Default.TabletAndroid, viewportConfig.deviceType == ViewportDeviceType.TABLET) { onSelectDeviceType(ViewportDeviceType.TABLET) }
            ViewportPresetChip("Desktop", Icons.Default.DesktopWindows, viewportConfig.deviceType == ViewportDeviceType.DESKTOP) { onSelectDeviceType(ViewportDeviceType.DESKTOP) }
            if (viewportConfig.deviceType != ViewportDeviceType.RESPONSIVE) {
                IconButton(onClick = onToggleOrientation, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.ScreenRotation, "Rotate", tint = if (viewportConfig.isLandscape) colors.accent else colors.textSecondary, modifier = Modifier.size(15.dp))
                }
                DimensionInputBox(customWidthText) { txt -> customWidthText = txt; txt.toIntOrNull()?.let { w -> onUpdateCustomSize(w, viewportConfig.heightPx) } }
                Text("×", color = colors.textMuted, fontSize = 11.sp)
                DimensionInputBox(customHeightText) { txt -> customHeightText = txt; txt.toIntOrNull()?.let { h -> onUpdateCustomSize(viewportConfig.widthPx, h) } }
                Text(if (viewportConfig.isLandscape) "Landscape" else "Portrait", color = colors.textMuted, fontSize = 10.sp)
            }
        }

        lastErrorBanner?.let { err ->
            Box(modifier = Modifier.fillMaxWidth().background(colors.error.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(err, color = colors.error, fontSize = 11.sp)
            }
        }

        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF111318)).padding(if (viewportConfig.deviceType == ViewportDeviceType.RESPONSIVE) 0.dp else 10.dp),
            contentAlignment = Alignment.Center
        ) {
            val isResponsive = viewportConfig.deviceType == ViewportDeviceType.RESPONSIVE
            val targetW = if (isResponsive) maxWidth else viewportConfig.effectiveWidth.dp.coerceAtMost(maxWidth)
            val targetH = if (isResponsive) maxHeight else viewportConfig.effectiveHeight.dp.coerceAtMost(maxHeight)

            Box(
                modifier = Modifier.width(targetW).height(targetH)
                    .clip(RoundedCornerShape(if (isResponsive) 0.dp else 8.dp))
                    .border(if (isResponsive) 0.dp else 1.5.dp, colors.accent.copy(alpha = 0.6f), RoundedCornerShape(if (isResponsive) 0.dp else 8.dp))
                    .background(Color.White)
            ) {
                if (projectRootPath == null || entryHtmlPath == null || !File(entryHtmlPath).exists()) {
                    Box(Modifier.fillMaxSize().background(colors.editorBg), contentAlignment = Alignment.Center) {
                        Text("Open or create an .html file (e.g. index.html) to preview", color = colors.textMuted, fontSize = 12.sp)
                    }
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                settings.apply {
                                    javaScriptEnabled = true; domStorageEnabled = true; allowFileAccess = true
                                    useWideViewPort = true; loadWithOverviewMode = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                }
                                webChromeClient = object : WebChromeClient() {
                                    override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
                                        val lvl = when (cm.messageLevel()) {
                                            ConsoleMessage.MessageLevel.ERROR -> "ERROR"
                                            ConsoleMessage.MessageLevel.WARNING -> "WARN"
                                            ConsoleMessage.MessageLevel.DEBUG -> "LOG"
                                            else -> "INFO"
                                        }
                                        onConsoleMessage(lvl, cm.message(), "${cm.sourceId()}:${cm.lineNumber()}")
                                        return true
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                                        val url = request.url
                                        if (url.host == "vcode.local") {
                                            val rel = url.path?.trimStart('/')?.ifBlank { "index.html" } ?: "index.html"
                                            val rootDir = File(projectRootPath).canonicalFile
                                            val target = File(rootDir, rel).canonicalFile
                                            if (target.path.startsWith(rootDir.path) && target.exists() && target.isFile) {
                                                return WebResourceResponse(guessMimeType(target.name), "UTF-8", FileInputStream(target))
                                            }
                                            return WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", emptyMap(), ByteArrayInputStream("404: $rel".toByteArray()))
                                        }
                                        return super.shouldInterceptRequest(view, request)
                                    }
                                    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                        lastErrorBanner = "Preview error ($errorCode): $description"
                                        onConsoleMessage("ERROR", description ?: "Load failed", failingUrl ?: "")
                                    }
                                }
                                loadUrl(baseVirtualUrl + targetRelative)
                                webViewRef = this
                            }
                        }
                    )
                }

                if (!isResponsive) {
                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd).background(colors.accent, RoundedCornerShape(topStart = 6.dp)).pointerInput(viewportConfig) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val dx = with(density) { dragAmount.x.toDp().value }.toInt()
                                val dy = with(density) { dragAmount.y.toDp().value }.toInt()
                                onUpdateCustomSize((viewportConfig.effectiveWidth + dx).coerceIn(220, 2000), (viewportConfig.effectiveHeight + dy).coerceIn(220, 2000))
                            }
                        }.padding(4.dp)
                    ) {
                        Icon(Icons.Default.DragIndicator, "Resize", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("${viewportConfig.effectiveWidth}×${viewportConfig.effectiveHeight}", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        if (showConsole) {
            Column(modifier = Modifier.fillMaxWidth().height(110.dp).background(Color(0xFF13151B)).border(0.5.dp, colors.border)) {
                Row(modifier = Modifier.fillMaxWidth().background(colors.sideBarBg).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("CONSOLE (${consoleLogs.size})", color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClearConsole, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.DeleteSweep, "Clear", tint = colors.textMuted, modifier = Modifier.size(14.dp)) }
                }
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(consoleLogs, key = { it.id }) { log ->
                        val clr = when (log.level) { "ERROR" -> colors.error; "WARN" -> colors.warning; else -> Color(0xFFD1D5DB) }
                        Text("[${log.level}] ${log.message}", color = clr, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewportPresetChip(label: String, icon: ImageVector?, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalVCodeColors.current
    Row(
        modifier = Modifier.background(if (selected) colors.accent.copy(alpha = 0.25f) else colors.editorBg, RoundedCornerShape(4.dp)).border(0.5.dp, if (selected) colors.accent else colors.border, RoundedCornerShape(4.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = if (selected) colors.accent else colors.textSecondary, modifier = Modifier.size(13.dp))
        Text(label, color = if (selected) colors.accent else colors.textPrimary, fontSize = 11.sp)
    }
}

@Composable
private fun DimensionInputBox(value: String, onValueChange: (String) -> Unit) {
    val colors = LocalVCodeColors.current
    Box(
        modifier = Modifier.width(50.dp).height(24.dp).background(colors.editorBg, RoundedCornerShape(4.dp)).border(0.5.dp, colors.border, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value, onValueChange = { s -> if (s.length <= 4 && s.all { it.isDigit() }) onValueChange(s) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(color = colors.textPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(colors.accent), modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun guessMimeType(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
    "html", "htm" -> "text/html"; "css" -> "text/css"; "js", "mjs" -> "application/javascript"; "json" -> "application/json"
    "svg" -> "image/svg+xml"; "png" -> "image/png"; "jpg", "jpeg" -> "image/jpeg"; "gif" -> "image/gif"; "webp" -> "image/webp"
    "txt", "md" -> "text/plain"; else -> URLConnection.guessContentTypeFromName(fileName) ?: "application/octet-stream"
}
