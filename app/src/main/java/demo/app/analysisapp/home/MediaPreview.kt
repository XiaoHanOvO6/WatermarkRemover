package demo.app.analysisapp.home

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import demo.app.analysisapp.data.ContentKind
import demo.app.analysisapp.data.ParseResult
import demo.app.analysisapp.data.SlideVideo
import demo.app.analysisapp.data.loadImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
@Composable
internal fun MediaPreview(
    result: ParseResult,
    videos: List<SlideVideo>,
    isMakingVideo: Boolean,
    isSaving: Boolean,
    onSaveSingleImage: (String) -> Unit,
    onSaveVideo: (Int) -> Unit,
    onSaveRemoteVideo: (Int) -> Unit
) {
    // 同一时间只播放一个视频，点另一个卡片就切过去
    var playingIndex by remember(videos, result.videoUrls) { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (result.kind) {
            ContentKind.Video -> {
                val videoUrls = result.videoUrls
                when {
                    videoUrls.isEmpty() -> StatusCard(text = "没有可预览的视频", isError = true)

                    videoUrls.size == 1 -> VideoPreview(videoUrl = videoUrls[0])

                    else -> {
                        Text(
                            text = "视频（横向滑动查看）",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(end = 4.dp)
                        ) {
                            itemsIndexed(videoUrls) { index, videoUrl ->
                                Box(
                                    modifier = Modifier
                                        .width(240.dp)
                                        .height(320.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { playingIndex = index }
                                ) {
                                    RemoteVideoCardPreview(
                                        videoUrl = videoUrl,
                                        isPlaying = playingIndex == index,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.Black.copy(alpha = 0.55f)
                                    ) {
                                        Text(
                                            text = "${index + 1} / ${videoUrls.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp,
                                                vertical = 3.dp
                                            )
                                        )
                                    }
                                    SaveIcon(
                                        isSaving = isSaving,
                                        contentDescription = "保存这个视频",
                                        modifier = Modifier.align(Alignment.BottomEnd),
                                        onClick = { onSaveRemoteVideo(index) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ContentKind.Images -> {
                when {
                    // 合成好的视频优先展示
                    videos.isNotEmpty() -> {
                        if (videos.size == 1) {
                            Text(
                                text = "视频",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val video = videos[0]
                            val aspect = aspectOf(video)
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val cardHeight = (maxWidth / aspect).coerceAtMost(420.dp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(cardHeight)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { playingIndex = 0 }
                                ) {
                                    LocalVideoPreview(
                                        path = video.path,
                                        isPlaying = playingIndex == 0,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    SaveIcon(
                                        isSaving = isSaving,
                                        contentDescription = "保存这个视频",
                                        modifier = Modifier.align(Alignment.BottomEnd),
                                        onClick = { onSaveVideo(0) }
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "视频（横向滑动查看）",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(end = 4.dp)
                            ) {
                                itemsIndexed(videos) { index, video ->
                                    val aspect = aspectOf(video)
                                    val cardWidth = (300.dp * aspect).coerceAtMost(240.dp)
                                    Box(
                                        modifier = Modifier
                                            .width(cardWidth)
                                            .height(cardWidth / aspect)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { playingIndex = index }
                                    ) {
                                        LocalVideoPreview(
                                            path = video.path,
                                            isPlaying = playingIndex == index,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.Black.copy(alpha = 0.55f)
                                        ) {
                                            Text(
                                                text = "${index + 1} / ${videos.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 3.dp
                                                )
                                            )
                                        }
                                        SaveIcon(
                                            isSaving = isSaving,
                                            contentDescription = "保存这个视频",
                                            modifier = Modifier.align(Alignment.BottomEnd),
                                            onClick = { onSaveVideo(index) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    isMakingVideo -> {
                        Text(
                            text = "正在合成视频…",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    result.imageUrls.isEmpty() -> {
                        StatusCard(text = "没有可预览的图片", isError = true)
                    }

                    result.imageUrls.size == 1 -> {
                        Text(
                            text = "图片",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        NetworkImagePreview(imageUrl = result.imageUrls[0])
                    }

                    else -> {
                        val urls = result.imageUrls
                        Text(
                            text = "图片（横向滑动查看）",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(end = 4.dp)
                        ) {
                            itemsIndexed(urls) { index, imageUrl ->
                                Box(
                                    modifier = Modifier
                                        .width(230.dp)
                                        .height(300.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                ) {
                                    NetworkImagePreview(
                                        imageUrl = imageUrl,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.Black.copy(alpha = 0.55f)
                                    ) {
                                        Text(
                                            text = "${index + 1} / ${urls.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp,
                                                vertical = 3.dp
                                            )
                                        )
                                    }
                                    SaveIcon(
                                        isSaving = isSaving,
                                        contentDescription = "单独保存这张图片",
                                        modifier = Modifier.align(Alignment.BottomEnd),
                                        onClick = { onSaveSingleImage(imageUrl) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ContentKind.Unsupported -> {
                StatusCard(text = "暂不支持预览 ${result.rawType}", isError = true)
            }
        }
    }
}

/** 视频画面的宽高比（宽 / 高），异常数据时按 3:4 处理 */
private fun aspectOf(video: SlideVideo): Float {
    if (video.width <= 0 || video.height <= 0) return 3f / 4f
    return video.width.toFloat() / video.height.toFloat()
}

/** 卡片右下角的保存图标 */
@Composable
private fun SaveIcon(
    isSaving: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(enabled = !isSaving) { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.FileDownload,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** 播放本地合成好的视频：isPlaying 为 true 时播放，否则暂停 */
@Composable
private fun LocalVideoPreview(
    path: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            VideoView(context).apply {
                tag = path
                setVideoPath(path)
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                    if (isPlaying) {
                        start()
                    } else {
                        // 没在播放的卡片也要显示出第一帧，否则是一块黑
                        seekTo(1)
                    }
                }
            }
        },
        update = { videoView ->
            if (videoView.tag != path) {
                videoView.tag = path
                videoView.setVideoPath(path)
            }
            if (isPlaying) {
                if (!videoView.isPlaying) videoView.start()
            } else {
                if (videoView.isPlaying) videoView.pause()
                if (videoView.currentPosition == 0) videoView.seekTo(1)
            }
        }
    )
}

/** 播放远程视频卡片：isPlaying 为 true 时播放，否则停在第一帧 */
@Composable
private fun RemoteVideoCardPreview(
    videoUrl: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            VideoView(context).apply {
                tag = videoUrl
                setVideoURI(Uri.parse(videoUrl))
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                    if (isPlaying) {
                        start()
                    } else {
                        seekTo(1)
                    }
                }
            }
        },
        update = { videoView ->
            if (videoView.tag != videoUrl) {
                videoView.tag = videoUrl
                videoView.setVideoURI(Uri.parse(videoUrl))
            }
            if (isPlaying) {
                if (!videoView.isPlaying) videoView.start()
            } else {
                if (videoView.isPlaying) videoView.pause()
                if (videoView.currentPosition == 0) videoView.seekTo(1)
            }
        }
    )
}

@Composable
private fun VideoPreview(videoUrl: String) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        factory = { context ->
            val videoView = VideoView(context)
            val mediaController = MediaController(context)
            mediaController.setAnchorView(videoView)
            videoView.setMediaController(mediaController)
            videoView.setVideoURI(Uri.parse(videoUrl))
            videoView.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                videoView.start()
            }
            videoView.tag = videoUrl
            videoView.requestFocus()
            videoView
        },
        update = { videoView ->
            if (videoView.tag != videoUrl) {
                videoView.tag = videoUrl
                videoView.setVideoURI(Uri.parse(videoUrl))
            }
        }
    )
}

@Composable
private fun NetworkImagePreview(
    imageUrl: String,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(280.dp)
) {
    val imageBitmapState = produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, imageUrl) {
        value = withContext(Dispatchers.IO) {
            loadImageBitmap(imageUrl)?.asImageBitmap()
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val imageBitmap = imageBitmapState.value
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            CircularProgressIndicator()
        }
    }
}
