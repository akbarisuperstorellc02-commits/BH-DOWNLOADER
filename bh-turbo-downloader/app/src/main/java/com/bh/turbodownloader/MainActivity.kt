package com.bh.turbodownloader

import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.documentfile.provider.DocumentFile
import com.bh.turbodownloader.download.DownloadCoordinator
import com.bh.turbodownloader.download.DownloadUiItem
import java.util.Locale

private val Base = Color(0xFF070B16)
private val SurfaceA = Color(0xFF111B2E)
private val SurfaceB = Color(0xFF1B2B45)
private val Mint = Color(0xFF55F0D1)
private val Blue = Color(0xFF638CFF)
private val Muted = Color(0xFF9AABC5)
private val White = Color(0xFFF4F7FF)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lang = getSharedPreferences("bh_settings", MODE_PRIVATE).getString("language", "en-US") ?: "en-US"
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Mint, secondary = Blue, background = Base, surface = SurfaceA, onBackground = White, onSurface = White)) { TurboApp() } }
    }
}

@Composable
private fun TurboApp() {
    val context = LocalContext.current
    val coordinator = remember { DownloadCoordinator.get(context) }
    val downloads by coordinator.items.collectAsState()
    var selected by remember { mutableIntStateOf(0) }
    var language by remember { mutableStateOf(context.getSharedPreferences("bh_settings", 0).getString("language", "en-US") ?: "en-US") }
    var folder by remember { mutableStateOf(context.getSharedPreferences("bh_settings", 0).getString("folder_label", "Download / TurboBH") ?: "Download / TurboBH") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }.onSuccess {
                coordinator.setDestinationTree(uri)
                folder = DocumentFile.fromTreeUri(context, uri)?.name ?: "Custom folder"
                context.getSharedPreferences("bh_settings", 0).edit().putString("folder_label", folder).apply()
            }
        }
    }
    val start: (String) -> String = { raw ->
        try { coordinator.start(raw.trim()); context.getString(R.string.download_added) }
        catch (e: Exception) { e.message ?: context.getString(R.string.download_failed) }
    }
    val changeLanguage: (String) -> Unit = { tag ->
        language = tag
        context.getSharedPreferences("bh_settings", 0).edit().putString("language", tag).apply()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
    Surface(Modifier.fillMaxSize(), color = Base) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.align(Alignment.TopEnd).size(300.dp).background(Brush.radialGradient(listOf(Color(0x334D7FFF), Color.Transparent))))
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (selected) {
                        0 -> HomeScreen(downloads, start) { selected = 2 }
                        1 -> BrowserScreen(start)
                        2 -> LibraryScreen(downloads, coordinator)
                        else -> SettingsScreen(folder, language, { picker.launch(null) }, changeLanguage)
                    }
                }
                NavigationBar(containerColor = Color(0xFF0B1425), contentColor = Mint, tonalElevation = 12.dp) {
                    listOf(
                        Triple(stringResource(R.string.home), Icons.Default.Bolt, 0),
                        Triple(stringResource(R.string.browser), Icons.Default.Explore, 1),
                        Triple(stringResource(R.string.files), Icons.Default.Folder, 2),
                        Triple(stringResource(R.string.settings), Icons.Default.Settings, 3)
                    ).forEach { (label, icon, index) ->
                        NavigationBarItem(selected == index, { selected = index }, icon = { Icon(icon, label) }, label = { Text(label, fontSize = 10.sp) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(kicker: String, title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(kicker.uppercase(Locale.US), color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
        Text(title, color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        if (subtitle != null) Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun HomeScreen(downloads: List<DownloadUiItem>, start: (String) -> String, viewAll: () -> Unit) {
    var url by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("BH TURBO", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.5.sp)
                Text(stringResource(R.string.downloader), color = White, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.tagline), color = Muted, fontSize = 12.sp)
            }
            Box(Modifier.size(54.dp).shadow(18.dp, CircleShape).background(Brush.linearGradient(listOf(Color(0xFF2D7180), Color(0xFF24345F))), CircleShape).border(1.dp, Color(0x8855F0D1), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Bolt, null, tint = Mint, modifier = Modifier.size(29.dp))
            }
        }
        GlassCard {
            val active = downloads.lastOrNull { it.status == "Downloading" }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, null, tint = Mint)
                Text(stringResource(R.string.turbo_engine), color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.weight(1f))
                Text("LIVE", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(active?.let { formatSpeed(it.speedBytesPerSecond) } ?: stringResource(R.string.built_speed), color = White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text(active?.fileName ?: stringResource(R.string.segmented_transfers), color = Muted, fontSize = 11.sp)
            if (active?.progress != null) LinearProgressIndicator(progress = { active.progress!! }, modifier = Modifier.fillMaxWidth(), color = Mint)
        }
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.new_download), color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Default.AddLink, null, tint = Mint)
            }
            OutlinedTextField(
                value = url, onValueChange = { url = it; message = "" }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, placeholder = { Text(stringResource(R.string.paste_url)) },
                leadingIcon = { Icon(Icons.Default.Link, null, tint = Mint) },
                trailingIcon = { if (url.isNotBlank()) IconButton(onClick = { url = ""; message = "" }) { Icon(Icons.Default.Close, stringResource(R.string.clear)) } },
                shape = RoundedCornerShape(16.dp)
            )
            Button(onClick = { message = start(url); if (message == "Download added to the queue" || message == "دانلود به فهرست اضافه شد") url = "" },
                enabled = url.isNotBlank(), modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Color(0xFF06201E))) {
                Icon(Icons.Default.Download, null); Text("  " + stringResource(R.string.start_download), fontWeight = FontWeight.Bold)
            }
            if (message.isNotBlank()) Text(message, color = Muted, fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.recent_downloads), color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.view_all), color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { viewAll() }.padding(8.dp))
        }
        if (downloads.isEmpty()) GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FolderOpen, null, tint = Mint, modifier = Modifier.size(32.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(stringResource(R.string.download_space), color = White, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.download_space_hint), color = Muted, fontSize = 11.sp)
                }
            }
        } else downloads.takeLast(3).reversed().forEach { DownloadCard(it, null) }
        Text(stringResource(R.string.maximum_mode), color = Color(0xFF647795), fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun BrowserScreen(start: (String) -> String) {
    var address by remember { mutableStateOf("https://www.google.com") }
    var web by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Header(stringResource(R.string.internal_browser), stringResource(R.string.explore_download))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(address, { address = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text(stringResource(R.string.website_address)) }, shape = RoundedCornerShape(14.dp))
            IconButton(onClick = {
                val target = if (address.startsWith("http://") || address.startsWith("https://")) address else "https://$address"
                address = target; web?.loadUrl(target)
            }) { Icon(Icons.Default.ArrowForward, stringResource(R.string.open_website), tint = Mint) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BrowserAction(Icons.Default.ArrowBack, stringResource(R.string.back), web?.canGoBack() == true) { web?.goBack() }
            BrowserAction(Icons.Default.ArrowForward, stringResource(R.string.forward), web?.canGoForward() == true) { web?.goForward() }
            BrowserAction(Icons.Default.Refresh, stringResource(R.string.refresh), true) { web?.reload() }
            BrowserAction(Icons.Default.Home, stringResource(R.string.home), true) { web?.loadUrl("https://www.google.com") }
        }
        if (notice.isNotBlank()) Text(notice, color = Muted, fontSize = 10.sp)
        Card(Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            AndroidView(factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true; settings.domStorageEnabled = true
                    settings.allowFileAccess = false; settings.allowContentAccess = false
                    settings.javaScriptCanOpenWindowsAutomatically = false; settings.setSupportMultipleWindows(false)
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    if (android.os.Build.VERSION.SDK_INT >= 26) settings.safeBrowsingEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) { if (url != null) address = url }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, title: String?) { pageTitle = title.orEmpty() }
                    }
                    setDownloadListener { u, _, _, _, _ -> notice = start(u) }
                    web = this; loadUrl(address)
                }
            }, update = { if (it.url != address && address.startsWith("http")) it.loadUrl(address) }, modifier = Modifier.fillMaxSize())
        }
        if (pageTitle.isNotBlank()) Text(pageTitle, color = Muted, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun BrowserAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, action: () -> Unit) {
    OutlinedButton(onClick = action, enabled = enabled, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)) {
        Icon(icon, label, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(3.dp)); Text(label, fontSize = 10.sp)
    }
}

@Composable
private fun LibraryScreen(downloads: List<DownloadUiItem>, coordinator: DownloadCoordinator) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Header(stringResource(R.string.my_library), stringResource(R.string.your_downloads))
        if (downloads.isEmpty()) GlassCard { Text(stringResource(R.string.no_downloads), color = Muted) }
        else downloads.reversed().forEach { DownloadCard(it, coordinator) }
    }
}

@Composable
private fun DownloadCard(item: DownloadUiItem, coordinator: DownloadCoordinator?) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (item.status == "Completed") Icons.Default.CheckCircle else Icons.Default.Downloading, null, tint = Mint)
            Column(Modifier.weight(1f).padding(start = 9.dp)) {
                Text(item.fileName, color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(statusLabel(item.status), color = Muted, fontSize = 10.sp)
            }
            if (item.speedBytesPerSecond > 0L) Text(formatSpeed(item.speedBytesPerSecond), color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (item.progress != null) {
            LinearProgressIndicator(progress = { item.progress!! }, modifier = Modifier.fillMaxWidth(), color = Mint)
            Row {
                Text(formatBytes(item.downloaded) + " / " + formatBytes(item.total), color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text(((item.progress!! * 100).toInt()).toString() + "%", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        } else if (item.status != "Completed") Text(stringResource(R.string.calculating_size), color = Muted, fontSize = 10.sp)
        item.error?.let { Text(it, color = Color(0xFFFF9292), fontSize = 10.sp) }
        if (coordinator != null) Row(verticalAlignment = Alignment.CenterVertically) {
            when (item.status) {
                "Downloading", "Queued", "Probing" -> Button(onClick = { coordinator.pause(item.id) }, colors = ButtonDefaults.buttonColors(containerColor = SurfaceB), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) {
                    Icon(Icons.Default.Pause, null, modifier = Modifier.size(15.dp)); Text(" " + stringResource(R.string.pause), fontSize = 10.sp)
                }
                "Paused", "Interrupted", "Failed" -> Button(onClick = { coordinator.resume(item.id) }, colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Color(0xFF06201E)), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(15.dp)); Text(" " + stringResource(R.string.resume), fontSize = 10.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { coordinator.removeFromHistory(item.id) }) { Icon(Icons.Default.DeleteOutline, stringResource(R.string.remove), tint = Color(0xFFFF9292)) }
        }
    }
}

@Composable
private fun SettingsScreen(folder: String, language: String, chooseFolder: () -> Unit, changeLanguage: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Header(stringResource(R.string.preferences), stringResource(R.string.settings))
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, null, tint = Mint)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(stringResource(R.string.download_mode), color = White, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.maximum_performance), color = Muted, fontSize = 11.sp)
                }
                Text("8x", color = Mint, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
        }
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, null, tint = Mint)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(stringResource(R.string.save_location), color = White, fontWeight = FontWeight.SemiBold)
                    Text(folder, color = Muted, fontSize = 10.sp, maxLines = 2)
                }
            }
            Button(onClick = chooseFolder, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Color(0xFF06201E))) {
                Icon(Icons.Default.FolderOpen, null); Text("  " + stringResource(R.string.choose_folder))
            }
        }
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, null, tint = Mint)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(stringResource(R.string.appearance), color = White, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.dark_glass), color = Muted, fontSize = 11.sp)
                }
                Icon(Icons.Default.CheckCircle, null, tint = Mint)
            }
        }
        LanguagePicker(language, changeLanguage)
        Text(stringResource(R.string.language_restart_hint), color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun LanguagePicker(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("en-US" to "English", "fa-AF" to "دری افغانستان", "ps-AF" to "پښتو", "ar" to "العربية")
    GlassCard {
        Text(stringResource(R.string.language), color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(languages.firstOrNull { it.first == selected }?.second ?: "English", modifier = Modifier.weight(1f))
                Icon(Icons.Default.ExpandMore, null)
            }
            DropdownMenu(expanded, { expanded = false }) {
                languages.forEach { (tag, name) ->
                    DropdownMenuItem(text = { Text(name) }, leadingIcon = { if (tag == selected) Icon(Icons.Default.Check, null, tint = Mint) },
                        onClick = { expanded = false; onSelect(tag) })
                }
            }
        }
    }
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(22.dp)), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceA),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x223C6586))) {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF17263C), Color(0xFF101A2C)))).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun statusLabel(status: String): String = when (status) {
    "Downloading" -> stringResource(R.string.downloading)
    "Completed" -> stringResource(R.string.completed)
    "Paused", "Pausing" -> stringResource(R.string.paused)
    "Failed" -> stringResource(R.string.failed)
    "Interrupted" -> stringResource(R.string.interrupted)
    "Queued" -> stringResource(R.string.queued)
    else -> status
}

private fun formatSpeed(value: Long): String = when {
    value >= 1024L * 1024L -> String.format(Locale.US, "%.2f MB/s", value / (1024.0 * 1024.0))
    value >= 1024L -> String.format(Locale.US, "%.1f KB/s", value / 1024.0)
    else -> String.format(Locale.US, "%d B/s", value)
}
private fun formatBytes(value: Long): String = when {
    value >= 1024L * 1024L * 1024L -> String.format(Locale.US, "%.2f GB", value / (1024.0 * 1024.0 * 1024.0))
    value >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", value / (1024.0 * 1024.0))
    value >= 1024L -> String.format(Locale.US, "%.1f KB", value / 1024.0)
    else -> String.format(Locale.US, "%d B", value)
}
