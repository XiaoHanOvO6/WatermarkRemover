package demo.app.analysisapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import demo.app.analysisapp.data.ApiChannel
import demo.app.analysisapp.settings.ThemeMode
import demo.app.analysisapp.theme.小寒去水印Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember {
                context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            }
            val initialMode = runCatching {
                ThemeMode.valueOf(prefs.getString("theme_mode", null).orEmpty())
            }.getOrDefault(ThemeMode.System)
            val initialColor = prefs.getString("theme_color", null) ?: "跟随系统"
            val initialChannel = runCatching {
                ApiChannel.valueOf(prefs.getString("api_channel", null).orEmpty())
            }.getOrDefault(ApiChannel.Line1)

            var themeMode by remember { mutableStateOf(initialMode) }
            var themeColor by remember { mutableStateOf(initialColor) }
            var apiChannel by remember { mutableStateOf(initialChannel) }

            val useDarkTheme = when (themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            小寒去水印Theme(darkTheme = useDarkTheme, themeColor = themeColor) {
                MainScreen(
                    themeMode = themeMode,
                    onThemeModeChange = {
                        themeMode = it
                        prefs.edit().putString("theme_mode", it.name).apply()
                    },
                    themeColor = themeColor,
                    onThemeColorChange = {
                        themeColor = it
                        prefs.edit().putString("theme_color", it).apply()
                    },
                    apiChannel = apiChannel,
                    onApiChannelChange = {
                        apiChannel = it
                        prefs.edit().putString("api_channel", it.name).apply()
                    }
                )
            }
        }
    }
}
