package demo.app.analysisapp.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import demo.app.analysisapp.data.ParseResult
import demo.app.analysisapp.data.SlideVideo
import demo.app.analysisapp.data.canSave
import demo.app.analysisapp.data.copyToClipboard
import demo.app.analysisapp.data.resourceSummary
import demo.app.analysisapp.data.saveButtonText
import demo.app.analysisapp.data.typeLabel
@Composable
internal fun ResultPanel(
    result: ParseResult,
    videos: List<SlideVideo>,
    isMakingVideo: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
    onSaveSingleImage: (String) -> Unit,
    onSaveVideo: (Int) -> Unit
) {
    val context = LocalContext.current
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.typeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = result.publishTime.ifBlank { result.rawType },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = result.title.ifBlank { "未命名作品" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            InfoLine(label = "作者", value = result.authorName.ifBlank { "未知" })
            InfoLine(label = "类型", value = result.resourceSummary)
            if (result.publishTime.isNotBlank()) {
                InfoLine(label = "发布时间", value = result.publishTime)
            }

            MediaPreview(
                result = result,
                videos = videos,
                isMakingVideo = isMakingVideo,
                isSaving = isSaving,
                onSaveSingleImage = onSaveSingleImage,
                onSaveVideo = onSaveVideo
            )

            CopyActions(result = result, context = context)

            ElevatedButton(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && result.canSave
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(18.dp)
                            .width(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存中")
                } else {
                    Text(result.saveButtonText)
                }
            }
        }
    }
}

@Composable
private fun CopyActions(result: ParseResult, context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "复制",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            result.videoUrl?.takeIf { it.isNotBlank() }?.let { videoUrl ->
                OutlinedButton(
                    onClick = { copyToClipboard(context, "视频链接", videoUrl) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("复制视频链接")
                }
            }

            result.title.takeIf { it.isNotBlank() && it != "未命名作品" }?.let { caption ->
                OutlinedButton(
                    onClick = { copyToClipboard(context, "文案", caption) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("复制文案")
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
