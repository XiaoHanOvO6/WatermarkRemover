package demo.app.analysisapp.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import demo.app.analysisapp.data.ParseResult
import demo.app.analysisapp.data.SlideVideo
import demo.app.analysisapp.data.canSave
import demo.app.analysisapp.data.copyToClipboard
import demo.app.analysisapp.data.resourceSummary
import demo.app.analysisapp.data.saveButtonText
import demo.app.analysisapp.data.typeLabel
import demo.app.analysisapp.data.videoUrl

@Composable
internal fun ResultPanel(
    result: ParseResult,
    videos: List<SlideVideo>,
    isMakingVideo: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
    onSaveSingleImage: (String) -> Unit,
    onSaveVideo: (Int) -> Unit,
    onSaveRemoteVideo: (Int) -> Unit
) {
    val context = LocalContext.current
    val videoUrl = result.videoUrl?.takeIf { it.isNotBlank() }
    val caption = result.title.takeIf { it.isNotBlank() && it != "未命名作品" }

    AppCard(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = result.typeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            if (result.publishTime.isNotBlank()) {
                Text(
                    text = result.publishTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = result.title.ifBlank { "未命名作品" },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = result.authorName.ifBlank { "未知作者" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "·",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = result.resourceSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        MediaPreview(
            result = result,
            videos = videos,
            isMakingVideo = isMakingVideo,
            isSaving = isSaving,
            onSaveSingleImage = onSaveSingleImage,
            onSaveVideo = onSaveVideo,
            onSaveRemoteVideo = onSaveRemoteVideo
        )

        if (videoUrl != null || caption != null) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                videoUrl?.let {
                    OutlinedButton(
                        onClick = { copyToClipboard(context, "视频链接", it) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Link,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("视频链接", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }

                caption?.let {
                    OutlinedButton(
                        onClick = { copyToClipboard(context, "文案", it) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("文案", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !isSaving && result.canSave,
            shape = MaterialTheme.shapes.medium
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("保存中", style = MaterialTheme.typography.labelLarge)
            } else {
                Text(
                    text = result.saveButtonText,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
