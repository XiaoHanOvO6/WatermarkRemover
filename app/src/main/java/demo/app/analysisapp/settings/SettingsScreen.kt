package demo.app.analysisapp.settings

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import demo.app.analysisapp.theme.SeedColors

enum class ThemeMode(val label: String) {
    System("跟随系统"),
    Light("浅色"),
    Dark("深色")
}

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeColor: String,
    onThemeColorChange: (String) -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 4.dp)
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
                )
            },
            modifier = Modifier.clickable { showColorDialog = true }
        )
        HorizontalDivider(modifier = Modifier.padding(start = 20.dp, end = 20.dp))

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
            supportingContent = { Text("小寒去水印 v2.0") }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
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

    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 28.dp, top = 18.dp, bottom = 4.dp)
    )
}
