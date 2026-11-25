package demo.app.analysisapp.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun InputPanel(
    inputText: String,
    extractedUrl: String?,
    isParsing: Boolean,
    onInputChange: (String) -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    onParse: () -> Unit
) {
    AppCard(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp),
            placeholder = {
                Text(
                    text = "粘贴抖音分享口令或链接",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            maxLines = 5,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            trailingIcon = {
                if (inputText.isNotEmpty() && !isParsing) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "清空内容",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )

        RecognizeHint(extractedUrl = extractedUrl, hasInput = inputText.isNotBlank())

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPaste,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                enabled = !isParsing,
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentPaste,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "粘贴",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    softWrap = false
                )
            }

            Button(
                onClick = onParse,
                modifier = Modifier
                    .weight(1.7f)
                    .height(48.dp),
                enabled = !isParsing,
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                if (isParsing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "解析中",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "开始解析",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        softWrap = false
                    )
                }
            }
        }
    }
}
