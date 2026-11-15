package demo.app.analysisapp.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import demo.app.analysisapp.data.ApiChannel
import demo.app.analysisapp.data.ParseResult
import demo.app.analysisapp.data.canMakeVideo
import demo.app.analysisapp.data.extractDouyinUrl
import demo.app.analysisapp.data.makeSlideVideos
import demo.app.analysisapp.data.readClipboardText
import demo.app.analysisapp.data.requestAnalysis
import demo.app.analysisapp.data.saveLocalVideo
import demo.app.analysisapp.data.saveParseResult
import demo.app.analysisapp.data.saveRemoteVideo
import demo.app.analysisapp.data.saveSingleImage
import demo.app.analysisapp.data.timestamp
import demo.app.analysisapp.theme.小寒去水印Theme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@Composable
internal fun DouyinParserScreen(state: HomeUiState, apiChannel: ApiChannel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun startSave(target: ParseResult) {
        scope.launch {
            state.isSaving = true
            state.saveMessage = null
            try {
                val savedCount = withContext(Dispatchers.IO) {
                    saveParseResult(context, target)
                }
                state.saveMessage = "已保存 $savedCount 个文件"
            } catch (error: Exception) {
                state.saveMessage = error.message ?: "保存失败"
            } finally {
                state.isSaving = false
            }
        }
    }

    fun startSaveImage(imageUrl: String) {
        scope.launch {
            state.isSaving = true
            state.saveMessage = null
            try {
                withContext(Dispatchers.IO) {
                    saveSingleImage(context, imageUrl)
                }
                state.saveMessage = "已保存图片"
            } catch (error: Exception) {
                state.saveMessage = error.message ?: "保存失败"
            } finally {
                state.isSaving = false
            }
        }
    }

    fun startSaveVideoAt(index: Int) {
        val video = state.videos.getOrNull(index) ?: return
        scope.launch {
            state.isSaving = true
            state.saveMessage = null
            try {
                withContext(Dispatchers.IO) {
                    saveLocalVideo(
                        context = context,
                        file = File(video.path),
                        displayName = "douyin_${timestamp()}_${index + 1}.mp4"
                    )
                }
                state.saveMessage = "视频已保存到相册"
            } catch (error: Exception) {
                state.saveMessage = error.message ?: "保存失败"
            } finally {
                state.isSaving = false
            }
        }
    }

    fun startSaveRemoteVideoAt(index: Int) {
        val videoUrl = state.result?.videoUrls?.getOrNull(index) ?: return
        scope.launch {
            state.isSaving = true
            state.saveMessage = null
            try {
                withContext(Dispatchers.IO) {
                    saveRemoteVideo(context, videoUrl, index)
                }
                state.saveMessage = "视频已保存到相册"
            } catch (error: Exception) {
                state.saveMessage = error.message ?: "保存失败"
            } finally {
                state.isSaving = false
            }
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val target = state.pendingSave
        val imageUrl = state.pendingSaveImageUrl
        val videoIndex = state.pendingSaveVideoIndex
        val remoteVideoIndex = state.pendingSaveRemoteVideoIndex
        state.pendingSave = null
        state.pendingSaveImageUrl = null
        state.pendingSaveVideoIndex = null
        state.pendingSaveRemoteVideoIndex = null
        if (granted) {
            when {
                target != null -> startSave(target)
                imageUrl != null -> startSaveImage(imageUrl)
                videoIndex != null -> startSaveVideoAt(videoIndex)
                remoteVideoIndex != null -> startSaveRemoteVideoAt(remoteVideoIndex)
            }
        } else {
            state.saveMessage = "没有存储权限，无法保存"
        }
    }

    fun requestSave(target: ParseResult) {
        val needsStoragePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED

        if (needsStoragePermission) {
            state.pendingSave = target
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            startSave(target)
        }
    }

    fun requestSaveImage(imageUrl: String) {
        val needsStoragePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED

        if (needsStoragePermission) {
            state.pendingSaveImageUrl = imageUrl
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            startSaveImage(imageUrl)
        }
    }

    fun requestSaveVideoAt(index: Int) {
        val needsStoragePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED

        if (needsStoragePermission) {
            state.pendingSaveVideoIndex = index
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            startSaveVideoAt(index)
        }
    }

    fun requestSaveRemoteVideoAt(index: Int) {
        val needsStoragePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED

        if (needsStoragePermission) {
            state.pendingSaveRemoteVideoIndex = index
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            startSaveRemoteVideoAt(index)
        }
    }

    val extractedUrl = remember(state.inputText) { extractDouyinUrl(state.inputText) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Header()

        InputPanel(
            inputText = state.inputText,
            extractedUrl = extractedUrl,
            isParsing = state.isParsing,
            onInputChange = {
                state.inputText = it
                state.errorMessage = null
                state.saveMessage = null
            },
            onPaste = {
                val clipText = readClipboardText(context)
                if (clipText.isNotBlank()) {
                    state.inputText = clipText
                    state.errorMessage = null
                    state.saveMessage = null
                }
            },
            onClear = {
                state.inputText = ""
                state.errorMessage = null
                state.saveMessage = null
            },
            onParse = {
                val url = extractedUrl
                if (url == null) {
                    state.errorMessage = "没有识别到抖音链接"
                    return@InputPanel
                }

                scope.launch {
                    state.isParsing = true
                    state.errorMessage = null
                    state.saveMessage = null
                    state.result = null
                    state.videos = emptyList()
                    try {
                        val parsed = withContext(Dispatchers.IO) {
                            requestAnalysis(url, apiChannel)
                        }
                        state.result = parsed
                        // 图文 / live：解析完直接把每张图合成成视频，界面上展示视频
                        if (parsed.canMakeVideo) {
                            state.isMakingVideo = true
                            val videos = withContext(Dispatchers.IO) {
                                makeSlideVideos(
                                    context = context,
                                    imageUrls = parsed.imageUrls,
                                    musicUrl = parsed.musicUrl
                                )
                            }
                            state.videos = videos
                            if (videos.isEmpty()) {
                                state.saveMessage = "视频合成失败，仍可保存静态图"
                            } else if (videos.size < parsed.imageUrls.size) {
                                state.saveMessage = "只合成成功 ${videos.size} 个视频"
                            }
                        }
                    } catch (error: Exception) {
                        state.result = null
                        state.videos = emptyList()
                        state.errorMessage = buildString {
                            append(apiChannel.label)
                            append("解析失败：")
                            append(error.message ?: "未知错误")
                            append("，可在设置里切换线路")
                        }
                    } finally {
                        state.isParsing = false
                        state.isMakingVideo = false
                    }
                }
            }
        )

        state.errorMessage?.let { StatusCard(text = it, isError = true) }
        state.saveMessage?.let { StatusCard(text = it, isError = false) }

        state.result?.let {
            ResultPanel(
                result = it,
                videos = state.videos,
                isMakingVideo = state.isMakingVideo,
                isSaving = state.isSaving,
                onSave = { requestSave(it) },
                onSaveSingleImage = { requestSaveImage(it) },
                onSaveVideo = { index -> requestSaveVideoAt(index) },
                onSaveRemoteVideo = { index -> requestSaveRemoteVideoAt(index) }
            )
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "抖音解析",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "粘贴分享口令或链接，自动提取无水印资源",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DouyinParserScreenPreview() {
    小寒去水印Theme {
        DouyinParserScreen(state = remember { HomeUiState() }, apiChannel = ApiChannel.Line1)
    }
}
