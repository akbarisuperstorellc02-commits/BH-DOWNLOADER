package com.bh.turbodownloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF0B1020)
private val Panel = Color(0xFF151D31)
private val Accent = Color(0xFF35D0BA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Accent,
                background = Night,
                surface = Panel,
                onBackground = Color(0xFFF4F7FF),
                onSurface = Color(0xFFF4F7FF)
            )) {
                DownloaderHome()
            }
        }
    }
}

@Composable
private fun DownloaderHome() {
    var url by remember { mutableStateOf("") }
    Surface(modifier = Modifier.fillMaxSize(), color = Night) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("BH TURBO", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Downloader", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Fast. Reliable. In control.", color = Color(0xFF9AA8C2), fontSize = 13.sp)
                }
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFFB9C5DA))
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("New download", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Paste a download link") },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) }
                    )
                    Button(
                        onClick = { /* Download flow is added in the engine milestone. */ },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = url.startsWith("https://") || url.startsWith("http://")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Text("  Start download")
                    }
                }
            }

            Text("DOWNLOADS", color = Color(0xFF9AA8C2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("No downloads yet", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    Text("Your active and completed files will appear here.", color = Color(0xFF9AA8C2), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Text("MAXIMUM ENGINE • INTERNAL BROWSER", modifier = Modifier.align(Alignment.CenterHorizontally), color = Color(0xFF64738F), fontSize = 10.sp)
        }
    }
}
