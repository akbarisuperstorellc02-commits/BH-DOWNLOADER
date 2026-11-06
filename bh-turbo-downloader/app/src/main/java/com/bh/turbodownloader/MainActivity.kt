package com.bh.turbodownloader

import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
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
        Box(Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF050914), Color(0xFF0B1426), Color(0xFF080D1B)))
        )) {
            AmbientBackdrop(Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (selected) {
                        0 -> HomeScreen(downloads, start) { selected = 2 }
                        1 -> BrowserScreen(start)
                        2 -> LibraryScreen(downloads, coordinator)
                        3 -> SettingsScreen(folder, language, { picker.launch(null) }, changeLanguage)
                        else -> ProfileScreen(downloads)
                    }
                }
                NavigationBar(containerColor = Color(0xFF0B1425), contentColor = Mint, tonalElevation = 12.dp) {
                    listOf(
                        Triple(stringResource(R.string.home), Icons.Default.Bolt, 0),
                        Triple(stringResource(R.string.browser), Icons.Default.Explore, 1),
                        Triple(stringResource(R.string.files), Icons.Default.Folder, 2),
                        Triple(stringResource(R.string.settings), Icons.Default.Settings, 3),
                        Triple(stringResource(R.string.profile), Icons.Default.Person, 4)
                    ).forEach { (label, icon, index) ->
                        NavigationBarItem(selected == index, { selected = index }, icon = { Icon(icon, label) }, label = { Text(label, fontSize = 10.sp) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AmbientBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x554B7DFF), Color(0x164B7DFF), Color.Transparent),
                center = Offset(w * 0.94f, h * 0.08f),
                radius = w * 0.72f
            ),
            radius = w * 0.72f,
            center = Offset(w * 0.94f, h * 0.08f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x4434DCC5), Color(0x1234DCC5), Color.Transparent),
                center = Offset(w * 0.02f, h * 0.76f),
                radius = w * 0.62f
            ),
            radius = w * 0.62f,
            center = Offset(w * 0.02f, h * 0.76f)
        )
        val step = 72f
        var x = 0f
        while (x <= w) {
            drawLine(Color(0x087FA8D8), Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
            x += step
        }
        var y = 0f
        while (y <= h) {
            drawLine(Color(0x087FA8D8), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            y += step
        }
        drawCircle(Color(0x223E9CFF), radius = w * 0.27f, center = Offset(w * 0.98f, h * 0.13f), style = Stroke(width = 1.2f))
        drawCircle(Color(0x1834DCC5), radius = w * 0.34f, center = Offset(w * 0.98f, h * 0.13f), style = Stroke(width = 1f))
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
            BhLogo(Modifier.size(62.dp))
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
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            DashboardStat(stringResource(R.string.failed), downloads.count { it.status == "Failed" }.toString(), Color(0xFFFF8B9B), Modifier.weight(1f))
            DashboardStat(stringResource(R.string.completed), downloads.count { it.status == "Completed" }.toString(), Color(0xFF55E6A5), Modifier.weight(1f))
            DashboardStat(stringResource(R.string.downloading), downloads.count { it.status == "Downloading" || it.status == "Queued" || it.status == "Probing" }.toString(), Mint, Modifier.weight(1f))
            DashboardStat(stringResource(R.string.total_downloads), downloads.size.toString(), White, Modifier.weight(1f))
        }
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Storage, null, tint = Mint)
                Text(stringResource(R.string.download_space), color = White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.save_location), color = Muted, fontSize = 10.sp)
            }
            LinearProgressIndicator(progress = { if (downloads.isEmpty()) 0f else downloads.count { it.status == "Completed" }.toFloat() / downloads.size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = Mint, trackColor = Color(0xFF263247))
            Text(stringResource(R.string.profile_activity) + "  •  " + downloads.size.toString(), color = Muted, fontSize = 10.sp)
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
            Button(onClick = { message = start(url); if (url.startsWith("http://", true) || url.startsWith("https://", true)) url = "" },
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
private fun DashboardStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.height(88.dp).clip(RoundedCornerShape(18.dp))
        .background(color.copy(alpha = 0.08f))
        .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
        .padding(horizontal = 5.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(label, color = Muted, fontSize = 8.sp, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun BhLogo(modifier: Modifier = Modifier) {
    Box(modifier.shadow(20.dp, RoundedCornerShape(20.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF102B3D), Color(0xFF17213C), Color(0xFF07121F))), RoundedCornerShape(20.dp))
        .border(1.dp, Color(0xAA55F0D1), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().padding(5.dp).border(1.dp, Color(0x334B7DFF), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Bolt, null, tint = Mint, modifier = Modifier.size(31.dp))
            Text("BH", color = White, fontSize = 8.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp))
        }
    }
}

@Composable
private fun ProfileScreen(downloads: List<DownloadUiItem>) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("bh_settings", 0) }
    var displayName by remember { mutableStateOf(prefs.getString("profile_name", "BH User") ?: "BH User") }
    var editing by remember { mutableStateOf(false) }
    val completed = downloads.count { it.status == "Completed" }
    val active = downloads.count { it.status == "Downloading" || it.status == "Queued" || it.status == "Probing" }
    val failed = downloads.count { it.status == "Failed" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Header(stringResource(R.string.profile), stringResource(R.string.my_profile), stringResource(R.string.profile_local_hint))
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(78.dp).shadow(12.dp, CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF1F9D9A), Color(0xFF263A75))), CircleShape)
                    .border(2.dp, Mint.copy(alpha = 0.8f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = White, modifier = Modifier.size(42.dp))
                    Box(Modifier.align(Alignment.BottomEnd).size(20.dp).background(Color(0xFF16C784), CircleShape)
                        .border(2.dp, SurfaceA, CircleShape))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(displayName, color = White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.profile_local_account), color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.profile_no_signin), color = Muted, fontSize = 10.sp)
                }
                IconButton(onClick = { editing = true }) { Icon(Icons.Default.Edit, stringResource(R.string.edit_profile), tint = Mint) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileStat(stringResource(R.string.completed), completed.toString(), Mint, Modifier.weight(1f))
                ProfileStat(stringResource(R.string.downloading), active.toString(), Blue, Modifier.weight(1f))
                ProfileStat(stringResource(R.string.failed), failed.toString(), Color(0xFFFF858F), Modifier.weight(1f))
            }
        }
        GlassCard {
            Text(stringResource(R.string.profile_activity), color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Downloading, null, tint = Mint)
                Text(stringResource(R.string.total_downloads), color = Muted, modifier = Modifier.weight(1f))
                Text(downloads.size.toString(), color = White, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, null, tint = Mint)
                Text(stringResource(R.string.profile_private_note), color = Muted, fontSize = 11.sp)
            }
        }
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BhLogo(Modifier.size(48.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("BH TURBO DOWNLOADER", color = White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    Text(stringResource(R.string.profile_version), color = Muted, fontSize = 10.sp)
                }
                Icon(Icons.Default.Bolt, null, tint = Mint, modifier = Modifier.size(24.dp))
            }
        }
        if (editing) {
            AlertDialog(
                onDismissRequest = { editing = false },
                title = { Text(stringResource(R.string.edit_profile), color = White) },
                text = { OutlinedTextField(value = displayName, onValueChange = { displayName = it.take(32) },
                    singleLine = true, label = { Text(stringResource(R.string.profile_name)) }) },
                confirmButton = { TextButton(onClick = {
                    val safeName = displayName.trim().ifBlank { "BH User" }
                    displayName = safeName
                    prefs.edit().putString("profile_name", safeName).apply()
                    editing = false
                }) { Text(stringResource(R.string.save), color = Mint) } },
                dismissButton = { TextButton(onClick = { editing = false }) { Text(stringResource(R.string.cancel)) } },
                containerColor = SurfaceA
            )
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.09f))
        .border(1.dp, color.copy(alpha = 0.32f), RoundedCornerShape(16.dp)).padding(11.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, color = color, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Muted, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun BrowserScreen(start: (String) -> String) {
    var address by remember { mutableStateOf("https://www.google.com") }
    var web by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf("") }
    var navVersion by remember { mutableIntStateOf(0) }
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
        val canBack = navVersion >= 0 && web?.canGoBack() == true
        val canForward = navVersion >= 0 && web?.canGoForward() == true
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BrowserAction(Icons.Default.ArrowBack, stringResource(R.string.back), canBack) { web?.goBack() }
            BrowserAction(Icons.Default.ArrowForward, stringResource(R.string.forward), canForward) { web?.goForward() }
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
                        override fun onPageFinished(view: WebView?, url: String?) { if (url != null) address = url; navVersion++ }
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
            if (item.status == "Downloading" || item.status == "Completed") Text(formatSpeed(item.speedBytesPerSecond), color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (item.progress != null) {
            LinearProgressIndicator(progress = { item.progress!! }, modifier = Modifier.fillMaxWidth(), color = Mint)
            Row {
                Text(formatBytes(item.downloaded) + " / " + formatBytes(item.total), color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text(((item.progress!! * 100).toInt()).toString() + "%", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            if (item.status == "Downloading" && item.speedBytesPerSecond > 0L && item.total > item.downloaded) {
                val secondsLeft = (item.total - item.downloaded) / item.speedBytesPerSecond
                Text(stringResource(R.string.eta) + ": " + formatDuration(secondsLeft), color = Muted, fontSize = 10.sp)
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
    Card(
        Modifier.fillMaxWidth().shadow(18.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC111D31)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(Color(0x6655F0D1), Color(0x334B7DFF), Color(0x2255F0D1)))
        )
    ) {
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xE61A2B43), Color(0xE60D1729), Color(0xE61B2440))))
                .padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
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

private fun formatDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val hours = safe / 3600L
    val minutes = (safe % 3600L) / 60L
    val secs = safe % 60L
    return if (hours > 0L) String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs)
    else String.format(Locale.US, "%02d:%02d", minutes, secs)
}
