package com.bh.turbodownloader

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
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
                    0 -> HomeScreen()
                    1 -> BrowserScreen()
                    2 -> LibraryScreen()
                    else -> SettingsScreen()
                }
                NavigationBar(
                    containerColor = Color(0xFF0D1628),
                    contentColor = Mint
                ) {
                    val items = listOf(
                        Triple("Home", Icons.Default.Bolt, 0),
                        Triple("Browser", Icons.Default.Explore, 1),
                        Triple("Files", Icons.Default.Folder, 2),
                        Triple("Settings", Icons.Default.Settings, 3)
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
private fun HomeScreen() {
    var url by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Ready for your next download") }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("BH TURBO", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("Downloader", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("Your files. At full speed.", color = Muted, fontSize = 12.sp)
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
                    Text("New download", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.MoreHoriz, null, tint = Muted)
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; message = "Link ready to check" },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Paste a file URL") },
                    leadingIcon = { Icon(Icons.Default.Language, null, tint = Mint) },
                    shape = RoundedCornerShape(16.dp)
                )
                Button(
                    onClick = {
                        message = if (url.startsWith("https://") || url.startsWith("http://")) {
                            "Link accepted • download engine connection is next"
                        } else "Enter a valid HTTP or HTTPS link"
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
            Text("RECENT DOWNLOADS", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
            Text("VIEW ALL", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                    Text("Your download space", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Downloaded files will appear here", color = Muted, fontSize = 11.sp)
                }
                Icon(Icons.Default.ArrowDownward, null, tint = Muted)
            }
        }
        Spacer(Modifier.weight(1f))
        Text("MAXIMUM MODE  •  SECURE CONNECTION", color = Color(0xFF60718F), fontSize = 9.sp, letterSpacing = 1.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
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
                Text("TURBO ENGINE", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.weight(1f))
                Box(Modifier.clip(CircleShape).background(Color(0x3354F0D0)).padding(horizontal = 9.dp, vertical = 5.dp)) {
                    Text("MAXIMUM", color = Mint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("Built for\nmaximum speed.", color = Color.White, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text("Smart connections • segmented transfers", color = Color(0xFFB7C8DE), fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FeatureChip("Multi-connection")
                FeatureChip("Resume support")
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
private fun BrowserScreen() {
    var address by remember { mutableStateOf("https://") }
    var currentUrl by remember { mutableStateOf("https://www.google.com") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("INTERNAL BROWSER", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text("Explore & download", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Enter website address") },
            leadingIcon = { Icon(Icons.Default.Language, null, tint = Mint) },
            shape = RoundedCornerShape(15.dp)
        )
        Button(onClick = {
            val target = if (address.startsWith("http://") || address.startsWith("https://")) address else "https://$address"
            currentUrl = target
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Color(0xFF06201E))) {
            Text("Open website")
        }
        Card(Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
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
private fun LibraryScreen() {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("MY LIBRARY", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text("Your downloads", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = SurfaceA)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Folder, null, tint = Mint, modifier = Modifier.size(42.dp))
                Text("Nothing here yet", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Your active, paused and completed downloads will be organized here.", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingsScreen() {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("PREFERENCES", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text("Settings", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        SettingRow("Download mode", "Maximum performance", Icons.Default.Bolt)
        SettingRow("Save location", "Download / TurboBH", Icons.Default.Folder)
        SettingRow("Appearance", "3D dark glass", Icons.Default.CheckCircle)
        SettingRow("Language", "English • Dari • Pashto • Arabic", Icons.Default.Language)
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
