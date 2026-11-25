package demo.app.analysisapp.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import demo.app.analysisapp.data.ApiChannel
import demo.app.analysisapp.theme.SeedColors

enum class ThemeMode(val label: String) {
    System("跟随系统"),
    Light("浅色"),
    Dark("深色")
}

private const val PROJECT_URL = "https://github.com/XiaoHanOvO6/WatermarkRemover"

@Composable
internal fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeColor: String,
    onThemeColorChange: (String) -> Unit,
    apiChannel: ApiChannel,
    onApiChannelChange: (ApiChannel) -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showChannelDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 8.dp)
        )

        SectionHeader("外观")
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            headlineContent = { Text("主题模式") },
            supportingContent = { Text(themeMode.label) },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.clickable { showThemeDialog = true }
        )

        ListItem(
            leadingContent = {
                Icon(
                    imageVector = Icons.Filled.Colorize,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            headlineContent = { Text("主题色") },
            supportingContent = { Text(themeColor) },
            trailingContent = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(colorPreview(themeColor))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
            },
            modifier = Modifier.clickable { showColorDialog = true }
        )

        HorizontalDivider(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )

        SectionHeader("解析")
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            headlineContent = { Text("解析线路") },
            supportingContent = { Text(apiChannel.label) },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.clickable { showChannelDialog = true }
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )

        SectionHeader("关于")
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            headlineContent = { Text("版本") },
            supportingContent = { Text("小寒去水印 v5.0") }
        )

        ListItem(
            leadingContent = {
                Icon(
                    imageVector = Icons.Filled.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            headlineContent = { Text("项目地址") },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.clickable { showLinkDialog = true }
        )
    }

    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            shape = MaterialTheme.shapes.large,
            title = { Text("外部链接") },
            text = { Text("即将在浏览器中打开：\n$PROJECT_URL") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLinkDialog = false
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_URL))
                            )
                        }
                    }
                ) {
                    Text("前往")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            shape = MaterialTheme.shapes.large,
            title = { Text("主题模式") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeModeChange(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = {
                                    onThemeModeChange(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(mode.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showChannelDialog) {
        AlertDialog(
            onDismissRequest = { showChannelDialog = false },
            shape = MaterialTheme.shapes.large,
            title = { Text("解析线路") },
            text = {
                Column {
                    ApiChannel.entries.forEach { channel ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onApiChannelChange(channel)
                                    showChannelDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = apiChannel == channel,
                                onClick = {
                                    onApiChannelChange(channel)
                                    showChannelDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(channel.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChannelDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            shape = MaterialTheme.shapes.large,
            title = { Text("主题色") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    ColorOptionRow(
                        name = "跟随系统",
                        swatch = MaterialTheme.colorScheme.primary,
                        selected = themeColor == "跟随系统",
                        onClick = {
                            onThemeColorChange("跟随系统")
                            showColorDialog = false
                        }
                    )
                    SeedColors.forEach { (name, color) ->
                        ColorOptionRow(
                            name = name,
                            swatch = color,
                            selected = themeColor == name,
                            onClick = {
                                onThemeColorChange(name)
                                showColorDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ColorOptionRow(
    name: String,
    swatch: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(swatch)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(name)
    }
}

@Composable
private fun colorPreview(name: String): Color {
    return if (name == "跟随系统") {
        MaterialTheme.colorScheme.primary
    } else {
        SeedColors[name] ?: MaterialTheme.colorScheme.primary
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp)
    )
}
