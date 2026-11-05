package com.bh.turbodownloader

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.LinearProgressIndicator
import com.bh.turbodownloader.download.DownloadCoordinator
import com.bh.turbodownloader.download.DownloadUiItem
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private val Base = Color(0xFF080D19)
private val SurfaceA = Color(0xFF111B2E)
private val SurfaceB = Color(0xFF182842)
private val Mint = Color(0xFF54F0D0)
private val Blue = Color(0xFF5C8DFF)
private val Muted = Color(0xFF91A2BE)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Mint,
                background = Base,
                surface = SurfaceA,
                onBackground = Color(0xFFF4F7FF),
                onSurface = Color(0xFFF4F7FF)
            )) {
                TurboApp()
            }
        }
    }
}

@Composable
private fun TurboApp() {
    var selected by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val coordinator = remember { DownloadCoordinator(context) }
    val downloads by coordinator.items.collectAsState()
    val downloadAddedMessage = stringResource(R.string.download_added)
    val linkReadyMessage = stringResource(R.string.link_ready)
    var selectedLanguage by remember {
        mutableStateOf(context.getSharedPreferences("bh_settings", 0).getString("language", "en-US") ?: "en-US")
    }
    val changeLanguage: (String) -> Unit = { tag ->
        selectedLanguage = tag
        context.getSharedPreferences("bh_settings", 0).edit().putString("language", tag).apply()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            coordinator.setDestinationTree(uri)
        }
    }
    val startDownload: (String) -> String = { link ->
        try {
            coordinator.start(link)
            downloadAddedMessage
        } catch (error: Exception) {
            error.message ?: "Could not start download"
        }
    }
    Surface(Modifier.fillMaxSize(), color = Base) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .size(280.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        Brush.radialGradient(listOf(Color(0x333D8BFF), Color.Transparent))
                    )
            )
            Column(Modifier.fillMaxSize()) {
                when (selected) {
                    0 -> HomeScreen(downloads, startDownload)
                    1 -> BrowserScreen(startDownload)
                    2 -> LibraryScreen(downloads, coordinator)
                    else -> SettingsScreen(onChooseFolder = { folderPicker.launch(null) }, selectedLanguage = selectedLanguage, onLanguageChange = changeLanguage)
                }
                NavigationBar(
                    containerColor = Color(0xFF0D1628),
                    contentColor = Mint
                ) {
                    val items = listOf(
                        Triple(stringResource(R.string.home), Icons.Default.Bolt, 0),
                        Triple(stringResource(R.string.browser), Icons.Default.Explore, 1),
                        Triple(stringResource(R.string.files), Icons.Default.Folder, 2),
                        Triple(stringResource(R.string.settings), Icons.Default.Settings, 3)
                    )
                    items.forEach { (label, icon, index) ->
                        NavigationBarItem(
                            selected = selected == index,
                            onClick = { selected = index },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(downloads: List<DownloadUiItem>, onStartDownload: (String) -> String) {
    var url by remember { mutableStateOf("") }
    var message by remember { mutableStateOf(stringResource(R.string.ready_download)) }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("BH TURBO", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text(stringResource(R.string.downloader), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.tagline), color = Muted, fontSize = 12.sp)
            }
            Box(
                Modifier.size(48.dp).shadow(14.dp, CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF285C72), Color(0xFF172B4B))), CircleShape)
                    .border(1.dp, Color(0x6654F0D0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Bolt, null, tint = Mint, modifier = Modifier.size(26.dp))
            }
        }

        HeroSpeedCard()

        Card(
            modifier = Modifier.fillMaxWidth().shadow(18.dp, RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceA)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.new_download), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.MoreHoriz, null, tint = Muted)
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; message = linkReadyMessage },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.paste_url)) },
                    leadingIcon = { Icon(Icons.Default.Language, null, tint = Mint) },
                    shape = RoundedCornerShape(16.dp)
                )
                Button(
                    onClick = {
                        message = onStartDownload(url)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = url.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Color(0xFF06201E))
                ) {
                    Icon(Icons.Default.Download, null)
                    Text("  Start download", fontWeight = FontWeight.Bold)
                }
                Text(message, color = Muted, fontSize = 11.sp)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.recent_downloads), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.view_all), color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceA)
        ) {
            Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                        .background(Color(0xFF203753)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Folder, null, tint = Mint) }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(stringResource(R.string.download_space), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(stringResource(R.string.download_space_hint), color = Muted, fontSize = 11.sp)
                }
                Icon(Icons.Default.ArrowDownward, null, tint = Muted)
            }
        }
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.maximum_mode), color = Color(0xFF60718F), fontSize = 9.sp, letterSpacing = 1.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun HeroSpeedCard() {
    Box(
        Modifier.fillMaxWidth().shadow(22.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1D4960), Color(0xFF182B50), Color(0xFF20254A))))
            .border(1.dp, Color(0x5554F0D0), RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Box(
            Modifier.size(150.dp).align(Alignment.CenterEnd)
                .background(Brush.radialGradient(listOf(Color(0x4454F0D0), Color.Transparent)), CircleShape)
        )
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, null, tint = Mint, modifier = Modifier.size(17.dp))
                Text(stringResource(R.string.turbo_engine), color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.weight(1f))
                Box(Modifier.clip(CircleShape).background(Color(0x3354F0D0)).padding(horizontal = 9.dp, vertical = 5.dp)) {
                    Text("MAXIMUM", color = Mint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("Built for\nmaximum speed.", color = Color.White, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.segmented_transfers), color = Color(0xFFB7C8DE), fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FeatureChip(stringResource(R.string.multi_connection))
                FeatureChip(stringResource(R.string.smart_fallback))
            }
        }
    }
}

@Composable
private fun FeatureChip(label: String) {
    Box(Modifier.clip(RoundedCornerShape(9.dp)).background(Color(0x33202E48)).border(1.dp, Color(0x3354F0D0), RoundedCornerShape(9.dp)).padding(horizontal = 9.dp, vertical = 6.dp)) {
        Text(label, color = Color(0xFFD7E5F5), fontSize = 9.sp)
    }
}

@Composable
private fun BrowserScreen(onStartDownload: (String) -> String) {
    var address by remember { mutableStateOf("https://") }
    var currentUrl by remember { mutableStateOf("https://www.google.com") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.internal_browser), color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text(stringResource(R.string.explore_download), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.website_address)) },
            leadingIcon = { Icon(Icons.Default.Language, null, tint = Mint) },
            shape = RoundedCornerShape(15.dp)
        )
        Button(onClick = {
            val target = if (address.startsWith("http://") || address.startsWith("https://")) address else "https://$address"
            currentUrl = target
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Color(0xFF06201E))) {
            Text(stringResource(R.string.open_website))
        }
        Card(Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.setSupportMultipleWindows(false)
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        if (android.os.Build.VERSION.SDK_INT >= 26) settings.safeBrowsingEnabled = true
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        setDownloadListener { downloadUrl, _, _, _, _ -> onStartDownload(downloadUrl) }
                        loadUrl(currentUrl)
                    }
                },
                update = { view -> if (view.url != currentUrl) view.loadUrl(currentUrl) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun LibraryScreen(downloads: List<DownloadUiItem>, coordinator: DownloadCoordinator) {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text(stringResource(R.string.my_library), color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text(stringResource(R.string.your_downloads), color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        if (downloads.isEmpty()) {
            Text(stringResource(R.string.no_downloads), color = Muted, fontSize = 12.sp)
        } else {
            downloads.reversed().forEach { item ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SurfaceA)) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(item.fileName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.status, color = Mint, fontSize = 10.sp, modifier = Modifier.weight(1f))
                            Text(item.progress?.let { (it * 100).toInt().toString() + "%" } ?: "…", color = Muted, fontSize = 10.sp)
                        }
                        if (item.progress != null) LinearProgressIndicator(progress = { item.progress!! }, modifier = Modifier.fillMaxWidth(), color = Mint)
                        item.error?.let { Text(it, color = Color(0xFFFF8A8A), fontSize = 10.sp) }
                        if (item.status in listOf("Downloading", "Pausing")) {
                            Button(onClick = { coordinator.pause(item.id) }, colors = ButtonDefaults.buttonColors(containerColor = SurfaceB)) {
                                Icon(Icons.Default.Pause, null)
                                Text(" Pause")
                            }
                        } else if (item.status in listOf("Paused", "Interrupted", "Failed")) {
                            Button(onClick = { coordinator.resume(item.id) }, colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Color(0xFF06201E))) {
                                Icon(Icons.Default.PlayArrow, null)
                                Text(" Resume")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(onChooseFolder: () -> Unit, selectedLanguage: String, onLanguageChange: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text(stringResource(R.string.preferences), color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text(stringResource(R.string.settings), color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        SettingRow(stringResource(R.string.download_mode), stringResource(R.string.maximum_performance), Icons.Default.Bolt)
        SettingRow(stringResource(R.string.save_location), "Download / TurboBH or selected folder", Icons.Default.Folder)
        Button(onClick = onChooseFolder, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Color(0xFF06201E))) {
            Icon(Icons.Default.Folder, null)
            Text("  " + stringResource(R.string.choose_folder), fontWeight = FontWeight.Bold)
        }
        SettingRow(stringResource(R.string.appearance), stringResource(R.string.dark_glass), Icons.Default.CheckCircle)
        LanguagePicker(selectedLanguage, onLanguageChange)
    }
}


@Composable
private fun LanguagePicker(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("en-US" to "English", "fa-AF" to "دری افغانستان", "ps-AF" to "پښتو", "ar" to "العربية")
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SurfaceA)) {
        Box(Modifier.fillMaxWidth().padding(16.dp)) {
            Column {
                Text(stringResource(R.string.language), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Button(onClick = { expanded = true }, colors = ButtonDefaults.buttonColors(containerColor = SurfaceB)) {
                    Text(languages.firstOrNull { it.first == selected }?.second ?: "English")
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                languages.forEach { (tag, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false; onSelect(tag) })
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SurfaceA)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Mint, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f).padding(start = 13.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(detail, color = Muted, fontSize = 10.sp)
            }
            Icon(Icons.Default.MoreHoriz, null, tint = Muted)
        }
    }
}
