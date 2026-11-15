package demo.app.analysisapp.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
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
            if (result.videoUrls.isEmpty()) throw IOException("没有可保存的视频直链")
            val multiple = result.videoUrls.size > 1
            result.videoUrls.forEachIndexed { index, videoUrl ->
                saveRemoteFile(
                    context = context,
                    sourceUrl = videoUrl,
                    displayName = "douyin_${timestamp()}${if (multiple) "_${index + 1}" else ""}.mp4",
                    mimeType = "video/mp4",
                    collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    relativePath = "${Environment.DIRECTORY_MOVIES}/小寒去水印"
                )
            }
            result.videoUrls.size
        }

        ContentKind.Images -> {
            if (result.imageUrls.isEmpty()) {
                throw IOException("没有可保存的图片")
            }
            var savedCount = 0
            var firstError: Exception? = null
            result.imageUrls.forEachIndexed { index, imageUrl ->
                try {
                    saveRemoteImageAsJpeg(
                        context = context,
                        sourceUrl = imageUrl,
                        displayName = "douyin_${timestamp()}_${index + 1}.jpg"
                    )
                    savedCount++
                } catch (error: Exception) {
                    // 单张失败不影响其他图片
                    if (firstError == null) firstError = error
                }
            }
            if (savedCount == 0) {
                throw IOException(firstError?.message ?: "图片保存失败")
            }
            savedCount
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

/**
 * 下载图片并统一转成 JPEG 保存。
 * 云汐接口返回的图片是 webp，直接按后缀存会导致部分相册打不开，所以这里解码后重新编码。
 */
internal fun saveRemoteImageAsJpeg(
    context: Context,
    sourceUrl: String,
    displayName: String
) {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/小寒去水印"
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: throw IOException("无法创建保存文件")
    try {
        val bitmap = loadImageBitmap(sourceUrl) ?: throw IOException("图片下载失败")
        try {
            resolver.openOutputStream(uri)?.use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) {
                    throw IOException("图片写入失败")
                }
            } ?: throw IOException("无法写入文件")
        } finally {
            bitmap.recycle()
        }

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

/** 保存单个远程视频：live 实况里每段真实视频单独保存 */
internal fun saveRemoteVideo(context: Context, videoUrl: String, index: Int) {
    saveRemoteFile(
        context = context,
        sourceUrl = videoUrl,
        displayName = "douyin_${timestamp()}_${index + 1}.mp4",
        mimeType = "video/mp4",
        collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        relativePath = "${Environment.DIRECTORY_MOVIES}/小寒去水印"
    )
}

internal fun saveSingleImage(context: Context, imageUrl: String) {
    saveRemoteImageAsJpeg(
        context = context,
        sourceUrl = imageUrl,
        displayName = "douyin_${timestamp()}.jpg"
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
