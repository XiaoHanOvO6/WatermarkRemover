package demo.app.analysisapp.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 保存解析结果到媒体库，返回保存的文件数 */
internal fun saveParseResult(context: Context, result: ParseResult): Int {
    return when (result.kind) {
        ContentKind.Video -> {
            val videoUrl = result.videoUrl ?: throw IOException("没有可保存的视频直链")
            saveRemoteFile(
                context = context,
                sourceUrl = videoUrl,
                displayName = "douyin_${timestamp()}.mp4",
                mimeType = "video/mp4",
                collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                relativePath = "${Environment.DIRECTORY_MOVIES}/小寒去水印"
            )
            1
        }

        ContentKind.Images -> {
            if (result.imageUrls.isEmpty()) {
                throw IOException("没有可保存的图片")
            }
            result.imageUrls.forEachIndexed { index, imageUrl ->
                saveRemoteFile(
                    context = context,
                    sourceUrl = imageUrl,
                    displayName = "douyin_${timestamp()}_${index + 1}.jpg",
                    mimeType = "image/jpeg",
                    collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    relativePath = "${Environment.DIRECTORY_PICTURES}/小寒去水印"
                )
            }
            result.imageUrls.size
        }

        ContentKind.Unsupported -> throw IOException("暂不支持保存 ${result.rawType}")
    }
}

/** 下载远程文件到媒体库 */
internal fun saveRemoteFile(
    context: Context,
    sourceUrl: String,
    displayName: String,
    mimeType: String,
    collection: Uri,
    relativePath: String
) {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    val uri = resolver.insert(collection, values) ?: throw IOException("无法创建保存文件")
    try {
        resolver.openOutputStream(uri)?.use { output ->
            val connection = URL(sourceUrl).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 60_000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                if (connection.responseCode !in 200..299) {
                    throw IOException("下载失败：HTTP ${connection.responseCode}")
                }
                connection.inputStream.use { input ->
                    input.copyTo(output)
                }
            } finally {
                connection.disconnect()
            }
        } ?: throw IOException("无法写入文件")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, this, null, null)
            }
        }
    } catch (error: Exception) {
        resolver.delete(uri, null, null)
        throw error
    }
}

internal fun saveSingleImage(context: Context, imageUrl: String) {
    saveRemoteFile(
        context = context,
        sourceUrl = imageUrl,
        displayName = "douyin_${timestamp()}.jpg",
        mimeType = "image/jpeg",
        collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        relativePath = "${Environment.DIRECTORY_PICTURES}/小寒去水印"
    )
}

/** 把本地文件写入相册的视频目录 */
internal fun saveLocalVideo(context: Context, file: File, displayName: String) {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_MOVIES}/小寒去水印"
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        ?: throw IOException("无法创建保存文件")
    try {
        resolver.openOutputStream(uri)?.use { output ->
            file.inputStream().use { input -> input.copyTo(output) }
        } ?: throw IOException("无法写入文件")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, this, null, null)
            }
        }
    } catch (error: Exception) {
        resolver.delete(uri, null, null)
        throw error
    }
}

internal fun timestamp(): String {
    return SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
}
