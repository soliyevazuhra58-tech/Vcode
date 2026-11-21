package com.vcode.ide.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import kotlin.math.roundToInt

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
    var zoomToFit by remember { mutableStateOf(true) }
    var customWidthText by remember(viewportConfig.effectiveWidth) {
        mutableStateOf(viewportConfig.effectiveWidth.toString())
    }
    var customHeightText by remember(viewportConfig.effectiveHeight) {
        mutableStateOf(viewportConfig.effectiveHeight.toString())
    }
    var lastErrorBanner by remember { mutableStateOf<String?>(null) }

    val currentProjectRoot by rememberUpdatedState(projectRootPath)
    val currentViewport by rememberUpdatedState(viewportConfig)
    val currentZoomToFit by rememberUpdatedState(zoomToFit)
    val currentOnConsole by rememberUpdatedState(onConsoleMessage)

    val baseVirtualUrl = "https://vcode.local/"
    val targetRelative = remember(projectRootPath, entryHtmlPath) {
        if (projectRootPath != null && entryHtmlPath != null) {
            runCatching { File(entryHtmlPath).relativeTo(File(projectRootPath)).path.replace(File.separatorChar, '/') }.getOrDefault("index.html")
        } else "index.html"
    }

    LaunchedEffect(
        refreshToken,
        targetRelative,
        projectRootPath,
        viewportConfig.deviceType,
        viewportConfig.effectiveWidth,
        viewportConfig.effectiveHeight,
        zoomToFit
    ) {
        lastErrorBanner = null
        webViewRef?.loadUrl(baseVirtualUrl + targetRelative)
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                destroy()
            }
            webViewRef = null
        }
    }

    Column(modifier = modifier.fillMaxSize().background(colors.panelBg).border(0.5.dp, colors.border)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(36.dp).background(colors.sideBarHeaderBg).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = { webViewRef?.goBack() }, modifier = Modifier.size(26.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textSecondary, modifier = Modifier.size(15.dp))
            }
            IconButton(onClick = { webViewRef?.goForward() }, modifier = Modifier.size(26.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Forward", tint = colors.textSecondary, modifier = Modifier.size(15.dp))
            }
            IconButton(onClick = onRefresh, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.Refresh, "Refresh", tint = colors.textSecondary, modifier = Modifier.size(15.dp))
            }
            Box(modifier = Modifier.weight(1f).background(colors.editorBg, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("vcode.local/$targetRelative", color = colors.textPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { showConsole = !showConsole }, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.Terminal, "Console", tint = if (showConsole) colors.accent else colors.textMuted, modifier = Modifier.size(15.dp))
            }
            IconButton(onClick = onClose, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.Close, "Close", tint = colors.textMuted, modifier = Modifier.size(15.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(colors.sideBarBg).border(0.5.dp, colors.border).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ViewportPresetChip("Fit", Icons.Default.AspectRatio, viewportConfig.deviceType == ViewportDeviceType.RESPONSIVE) {
                onSelectDeviceType(ViewportDeviceType.RESPONSIVE)
            }
            ViewportPresetChip("Mobile", Icons.Default.PhoneAndroid, viewportConfig.deviceType == ViewportDeviceType.MOBILE) {
                onSelectDeviceType(ViewportDeviceType.MOBILE)
            }
            ViewportPresetChip("Tablet", Icons.Default.TabletAndroid, viewportConfig.deviceType == ViewportDeviceType.TABLET) {
                onSelectDeviceType(ViewportDeviceType.TABLET)
            }
            ViewportPresetChip("Desktop", Icons.Default.DesktopWindows, viewportConfig.deviceType == ViewportDeviceType.DESKTOP) {
                onSelectDeviceType(ViewportDeviceType.DESKTOP)
            }
            ViewportPresetChip("Custom", Icons.Default.Tune, viewportConfig.deviceType == ViewportDeviceType.CUSTOM) {
                onSelectDeviceType(ViewportDeviceType.CUSTOM)
            }
            if (viewportConfig.deviceType != ViewportDeviceType.RESPONSIVE) {
                IconButton(onClick = onToggleOrientation, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.ScreenRotation, "Rotate", tint = if (viewportConfig.isLandscape) colors.accent else colors.textSecondary, modifier = Modifier.size(15.dp))
                }
                DimensionInputBox(customWidthText) { txt ->
                    customWidthText = txt
                    txt.toIntOrNull()?.let { w -> onUpdateCustomSize(w, viewportConfig.effectiveHeight) }
                }
                Text("×", color = colors.textMuted, fontSize = 11.sp)
                DimensionInputBox(customHeightText) { txt ->
                    customHeightText = txt
                    txt.toIntOrNull()?.let { h -> onUpdateCustomSize(viewportConfig.effectiveWidth, h) }
                }
                ViewportPresetChip(
                    label = if (zoomToFit) "Scale Fit" else "100%",
                    icon = null,
                    selected = zoomToFit
                ) { zoomToFit = !zoomToFit }
                Text(if (viewportConfig.isLandscape) "Landscape" else "Portrait", color = colors.textMuted, fontSize = 10.sp)
            }
        }

        lastErrorBanner?.let { err ->
            Box(modifier = Modifier.fillMaxWidth().background(colors.error.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(err, color = colors.error, fontSize = 11.sp)
            }
        }

        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF111318)),
            contentAlignment = if (viewportConfig.deviceType == ViewportDeviceType.RESPONSIVE || zoomToFit) Alignment.Center else Alignment.TopStart
        ) {
            val isResponsive = viewportConfig.deviceType == ViewportDeviceType.RESPONSIVE
            val effW = viewportConfig.effectiveWidth.coerceAtLeast(220)
            val effH = viewportConfig.effectiveHeight.coerceAtLeast(220)

            val fitScale = if (isResponsive || !zoomToFit) {
                1f
            } else {
                val availW = (maxWidth.value - 20f).coerceAtLeast(160f)
                val availH = (maxHeight.value - 20f).coerceAtLeast(160f)
                minOf(availW / effW.toFloat(), availH / effH.toFloat(), 1f)
            }

            val targetW = if (isResponsive) maxWidth else (effW * fitScale).dp
            val targetH = if (isResponsive) maxHeight else (effH * fitScale).dp

            val containerModifier = if (!isResponsive && !zoomToFit) {
                Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp)
            } else {
                Modifier
                    .fillMaxSize()
                    .padding(if (isResponsive) 0.dp else 10.dp)
            }

            Box(
                modifier = containerModifier,
                contentAlignment = if (isResponsive || zoomToFit) Alignment.Center else Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .width(targetW)
                        .height(targetH)
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
                                    setInitialScale(0)
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        allowFileAccess = true
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                        textZoom = 100
                                        cacheMode = WebSettings.LOAD_NO_CACHE
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
                                            currentOnConsole(lvl, cm.message(), "${cm.sourceId()}:${cm.lineNumber()}")
                                            return true
                                        }
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                                            val url = request.url
                                            val activeRoot = currentProjectRoot
                                            if (url.host == "vcode.local" && activeRoot != null) {
                                                val rel = url.path?.trimStart('/')?.ifBlank { "index.html" } ?: "index.html"
                                                val rootDir = File(activeRoot).canonicalFile
                                                val target = File(rootDir, rel).canonicalFile
                                                if (target.path.startsWith(rootDir.path) && target.exists() && target.isFile) {
                                                    val mime = guessMimeType(target.name)
                                                    val noStoreHeaders = mapOf("Cache-Control" to "no-store, no-cache, must-revalidate")
                                                    if (mime == "text/html") {
                                                        val rawHtml = runCatching { target.readText(Charsets.UTF_8) }.getOrNull()
                                                        if (rawHtml != null) {
                                                            val vp = currentViewport
                                                            val injected = injectSimulatedViewportMeta(
                                                                html = rawHtml,
                                                                deviceType = vp.deviceType,
                                                                widthCssPx = vp.effectiveWidth.coerceAtLeast(220),
                                                                zoomToFit = currentZoomToFit
                                                            )
                                                            return WebResourceResponse(
                                                                "text/html",
                                                                "UTF-8",
                                                                200,
                                                                "OK",
                                                                noStoreHeaders,
                                                                ByteArrayInputStream(injected.toByteArray(Charsets.UTF_8))
                                                            )
                                                        }
                                                    }
                                                    return WebResourceResponse(
                                                        mime,
                                                        "UTF-8",
                                                        200,
                                                        "OK",
                                                        noStoreHeaders,
                                                        FileInputStream(target)
                                                    )
                                                }
                                                return WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", emptyMap(), ByteArrayInputStream("404: $rel".toByteArray()))
                                            }
                                            return super.shouldInterceptRequest(view, request)
                                        }

                                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                            if (request?.isForMainFrame == true) {
                                                val desc = error?.description?.toString() ?: "Load failed"
                                                val code = error?.errorCode ?: -1
                                                lastErrorBanner = "Preview error ($code): $desc"
                                                currentOnConsole("ERROR", desc, request.url?.toString() ?: "")
                                            }
                                        }
                                    }
                                    loadUrl(baseVirtualUrl + targetRelative)
                                    webViewRef = this
                                }
                            },
                            update = { view ->
                                val scalePercent = if (isResponsive || !zoomToFit) {
                                    0
                                } else {
                                    0
                                }
                                view.settings.loadWithOverviewMode = isResponsive || zoomToFit
                                view.setInitialScale(scalePercent)
                            }
                        )
                    }

                    if (!isResponsive) {
                        Box(
                            modifier = Modifier.align(Alignment.BottomEnd).background(colors.accent, RoundedCornerShape(topStart = 6.dp)).pointerInput(effW, effH, fitScale) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val dx = (with(density) { dragAmount.x.toDp().value } / fitScale).roundToInt()
                                    val dy = (with(density) { dragAmount.y.toDp().value } / fitScale).roundToInt()
                                    onUpdateCustomSize((effW + dx).coerceIn(220, 2000), (effH + dy).coerceIn(220, 2000))
                                }
                            }.padding(4.dp)
                        ) {
                            Icon(Icons.Default.DragIndicator, "Resize", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            val pct = (fitScale * 100f).roundToInt()
                            val label = if (fitScale < 0.99f) "$effW×$effH ($pct%)" else "$effW×$effH"
                            Text(label, color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
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

private fun injectSimulatedViewportMeta(
    html: String,
    deviceType: ViewportDeviceType,
    widthCssPx: Int,
    zoomToFit: Boolean
): String {
    val contentAttr = when {
        deviceType == ViewportDeviceType.RESPONSIVE -> "width=device-width, initial-scale=1.0"
        zoomToFit -> "width=$widthCssPx"
        else -> "width=$widthCssPx, initial-scale=1.0"
    }
    val metaTag = "<meta name=\"viewport\" content=\"$contentAttr\" />"
    val existingViewportRegex = Regex("<meta\\b[^>]*name\\s*=\\s*[\"']viewport[\"'][^>]*>", RegexOption.IGNORE_CASE)
    val existingMatch = existingViewportRegex.find(html)
    if (existingMatch != null) {
        return html.substring(0, existingMatch.range.first) + metaTag + html.substring(existingMatch.range.last + 1)
    }
    val headMatch = Regex("<head\\b[^>]*>", RegexOption.IGNORE_CASE).find(html)
    return if (headMatch != null) {
        val insertAt = headMatch.range.last + 1
        html.substring(0, insertAt) + "\n" + metaTag + html.substring(insertAt)
    } else {
        "<head>$metaTag</head>\n$html"
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
