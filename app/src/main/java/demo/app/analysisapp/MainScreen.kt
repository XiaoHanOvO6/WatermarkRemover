package demo.app.analysisapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import demo.app.analysisapp.data.ApiChannel
import demo.app.analysisapp.home.DouyinParserScreen
import demo.app.analysisapp.home.HomeUiState
import demo.app.analysisapp.settings.SettingsScreen
import demo.app.analysisapp.settings.ThemeMode

private enum class MainTab(val label: String, val icon: ImageVector) {
    Home("首页", Icons.Filled.Home),
    Settings("设置", Icons.Filled.Settings)
}

@Composable
internal fun MainScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeColor: String,
    onThemeColorChange: (String) -> Unit,
    apiChannel: ApiChannel,
    onApiChannelChange: (ApiChannel) -> Unit
) {
    var selectedTab by remember { mutableStateOf(MainTab.Home) }
    val homeState = remember { HomeUiState() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedTab) {
                MainTab.Home -> DouyinParserScreen(state = homeState, apiChannel = apiChannel)
                MainTab.Settings -> SettingsScreen(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    themeColor = themeColor,
                    onThemeColorChange = onThemeColorChange,
                    apiChannel = apiChannel,
                    onApiChannelChange = onApiChannelChange
                )
            }
        }
    }
}
