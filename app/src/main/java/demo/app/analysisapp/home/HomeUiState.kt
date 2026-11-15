package demo.app.analysisapp.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import demo.app.analysisapp.data.ParseResult
import demo.app.analysisapp.data.SlideVideo

/**
 * 首页（抖音解析页）的 UI 状态。
 * 由 MainScreen 持有，切到设置页再切回首页时不丢失。
 */
internal class HomeUiState {
    var inputText by mutableStateOf("")
    var isParsing by mutableStateOf(false)
    var isSaving by mutableStateOf(false)
    var result by mutableStateOf<ParseResult?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var saveMessage by mutableStateOf<String?>(null)
    var pendingSave by mutableStateOf<ParseResult?>(null)
    var pendingSaveImageUrl by mutableStateOf<String?>(null)
    var pendingSaveVideoIndex by mutableStateOf<Int?>(null)
    var pendingSaveRemoteVideoIndex by mutableStateOf<Int?>(null)

    /** 解析完成后自动合成好的视频，图文 / live 才有 */
    var videos by mutableStateOf<List<SlideVideo>>(emptyList())
    var isMakingVideo by mutableStateOf(false)
}
