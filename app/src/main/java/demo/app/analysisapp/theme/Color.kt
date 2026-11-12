package demo.app.analysisapp.theme

import androidx.compose.ui.graphics.Color

// 兜底默认色（无种子色选择时使用）
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// MD3 种子色调色板：选一个主色，用 HCT 算法生成整套协调的 MD3 配色。
// "跟随系统" 走系统壁纸动态取色（Android 12+），否则用所选种子色生成。
val SeedColors: Map<String, Color> = mapOf(
    "青色" to Color(0xFF006A6A),
    "樱花粉" to Color(0xFF9C4277),
    "正红" to Color(0xFFBA1A1A),
    "橘橙" to Color(0xFF8B5000),
    "深绿" to Color(0xFF006E1C),
    "天蓝" to Color(0xFF0061A4),
    "深紫" to Color(0xFF6750A4),
    "棕色" to Color(0xFF8F4C38),
    "靛蓝" to Color(0xFF3F51B5),
    "薄荷绿" to Color(0xFF006B5D),
    "酒红" to Color(0xFF8F495B),
    "初音绿" to Color(0xFF39C5BB),
    "天依蓝" to Color(0xFF66CCFF),
    "巡音紫" to Color(0xFFCC99CC),
    "镜音橙" to Color(0xFFFFC04D)
)
