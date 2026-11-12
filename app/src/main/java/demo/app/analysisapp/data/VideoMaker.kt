package demo.app.analysisapp.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer

/** 合成好的一个视频：本地路径 + 画面尺寸（界面上用来按比例显示） */
internal data class SlideVideo(
    val path: String,
    val width: Int,
    val height: Int
)

/**
 * 把「图文 / live」的图片 + 背景音乐合成一个 MP4。
 *
 * 思路：
 * 1. 把每张图缩放成固定尺寸，转成 YUV420，逐帧喂给 H.264 编码器（多数手机第一张图会被复用 3 秒）；
 * 2. 背景音乐先解码成 PCM，再编码成 AAC；
 * 3. 音视频样本先收在内存里，最后一次性写进 MediaMuxer。
 *
 * 成功返回视频的宽高，失败返回 null（不影响调用方继续用别的方案）。
 */
internal fun makeSlideVideo(
    imageUrls: List<String>,
    musicUrl: String,
    cacheDir: File,
    output: File,
    secondsPerImage: Int = 3
): Pair<Int, Int>? {
    if (imageUrls.isEmpty()) return null

    val bitmaps = ArrayList<Bitmap>()
    try {
        for (url in imageUrls) {
            if (bitmaps.size >= MAX_IMAGES) break
            loadBitmapForVideo(url)?.let { bitmaps.add(it) }
        }
        if (bitmaps.isEmpty()) return null

        val (width, height) = chooseVideoSize(bitmaps[0])
        val framesPerImage = secondsPerImage * FRAMES_PER_SECOND

        val videoTrack = encodeVideo(bitmaps, width, height, framesPerImage) ?: return null
        val audioTrack = if (musicUrl.isNotBlank()) {
            runCatching { encodeAudio(musicUrl, cacheDir, videoTrack.durationUs) }.getOrNull()
        } else {
            null
        }

        mux(videoTrack, audioTrack, output)
        return if (output.length() > 0L) width to height else null
    } finally {
        bitmaps.forEach { if (!it.isRecycled) it.recycle() }
    }
}

/**
 * 每张图片各合成一个短视频（默认每张 3 秒，都带同一首背景音乐），
 * 文件放在应用缓存目录，返回按图片顺序生成成功的文件。
 *
 * 解析完成后立刻调用，界面上直接就是可以播放的视频。
 */
internal fun makeSlideVideos(
    context: Context,
    imageUrls: List<String>,
    musicUrl: String,
    secondsPerImage: Int = 3
): List<SlideVideo> {
    // 先清掉上一次合成的缓存，避免越积越多
    context.cacheDir.listFiles { file ->
        file.name.startsWith("slide_") && file.name.endsWith(".mp4")
    }?.forEach { it.delete() }

    val results = ArrayList<SlideVideo>()
    imageUrls.take(MAX_IMAGES).forEachIndexed { index, imageUrl ->
        val file = File(context.cacheDir, "slide_${timestamp()}_${index + 1}.mp4")
        val size = runCatching {
            makeSlideVideo(
                imageUrls = listOf(imageUrl),
                musicUrl = musicUrl,
                cacheDir = context.cacheDir,
                output = file,
                secondsPerImage = secondsPerImage
            )
        }.getOrNull()
        if (size != null && file.length() > 0L) {
            results.add(
                SlideVideo(
                    path = file.absolutePath,
                    width = size.first,
                    height = size.second
                )
            )
        } else {
            file.delete()
        }
    }
    return results
}

// ---------------------------------------------------------------------------
// 视频轨
// ---------------------------------------------------------------------------

private fun encodeVideo(
    bitmaps: List<Bitmap>,
    width: Int,
    height: Int,
    framesPerImage: Int
): EncodedTrack? {
    val bitrate = (width * height * FRAMES_PER_SECOND * 0.1f).toInt()
        .coerceIn(2_000_000, 10_000_000)
    val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
        setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
        )
        setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        setInteger(MediaFormat.KEY_FRAME_RATE, FRAMES_PER_SECOND)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
    }

    val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    val samples = ArrayList<Sample>()
    val bufferInfo = MediaCodec.BufferInfo()
    var outputFormat: MediaFormat? = null

    try {
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        // 每张图只做一次 YUV 转换，后面每一帧只是把字节拷进编码器
        val frames = bitmaps.map { toYuvPlanes(it, width, height) }
        val totalFrames = bitmaps.size * framesPerImage
        val frameDurationUs = 1_000_000L / FRAMES_PER_SECOND
        var frameIndex = 0

        while (frameIndex < totalFrames) {
            val inputIndex = codec.dequeueInputBuffer(10_000)
            if (inputIndex < 0) {
                outputFormat = drain(codec, samples, bufferInfo) ?: outputFormat
                continue
            }
            val image = codec.getInputImage(inputIndex)
                ?: throw IOException("当前设备不支持 YUV420 硬件编码")
            fillImage(image, frames[frameIndex / framesPerImage], width, height)
            codec.queueInputBuffer(
                inputIndex,
                0,
                width * height * 3 / 2,
                frameIndex * frameDurationUs,
                0
            )
            frameIndex++
            outputFormat = drain(codec, samples, bufferInfo) ?: outputFormat
        }

        val endIndex = codec.dequeueInputBuffer(20_000)
        if (endIndex >= 0) {
            codec.queueInputBuffer(
                endIndex,
                0,
                0,
                frameIndex * frameDurationUs,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM
            )
        }
        outputFormat = drain(codec, samples, bufferInfo, waitEndOfStream = true) ?: outputFormat
    } finally {
        runCatching { codec.stop() }
        codec.release()
    }

    val finalFormat = outputFormat ?: return null
    if (samples.isEmpty()) return null
    return EncodedTrack(finalFormat, samples)
}

// ---------------------------------------------------------------------------
// 音轨
// ---------------------------------------------------------------------------

private fun encodeAudio(musicUrl: String, cacheDir: File, durationUs: Long): EncodedTrack? {
    val musicFile = File(cacheDir, "music_${System.currentTimeMillis()}")
    try {
        download(musicUrl, musicFile)
        val pcm = decodePcm(musicFile) ?: return null
        return encodeAac(pcm, durationUs)
    } finally {
        musicFile.delete()
    }
}

/** 把音频文件解码成 PCM 数据 */
private fun decodePcm(file: File): PcmData? {
    val extractor = MediaExtractor()
    var trackIndex = -1
    var trackFormat: MediaFormat? = null
    try {
        extractor.setDataSource(file.absolutePath)
        for (index in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/")) {
                trackIndex = index
                trackFormat = format
                break
            }
        }
        val format = trackFormat ?: return null
        extractor.selectTrack(trackIndex)

        val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        val chunks = ArrayList<Sample>()
        val info = MediaCodec.BufferInfo()
        var sampleRate = 44_100
        var channels = 2
        try {
            decoder.configure(format, null, null, 0)
            decoder.start()

            var inputDone = false
            var outputDone = false
            while (!outputDone && chunks.size < MAX_PCM_CHUNKS) {
                if (!inputDone) {
                    val inputIndex = decoder.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val buffer = decoder.getInputBuffer(inputIndex)
                        val size = if (buffer != null) extractor.readSampleData(buffer, 0) else -1
                        if (size < 0) {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = decoder.dequeueOutputBuffer(info, 10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        sampleRate = decoder.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = decoder.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }

                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit

                    else -> if (outputIndex >= 0) {
                        if (info.size > 0) {
                            val buffer = decoder.getOutputBuffer(outputIndex)
                            if (buffer != null) {
                                buffer.position(info.offset)
                                buffer.limit(info.offset + info.size)
                                val data = ByteArray(info.size)
                                buffer.get(data)
                                chunks.add(Sample(data, info.presentationTimeUs, 0))
                            }
                        }
                        decoder.releaseOutputBuffer(outputIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }
            }
        } finally {
            runCatching { decoder.stop() }
            decoder.release()
        }

        if (chunks.isEmpty()) return null
        return PcmData(sampleRate, channels, chunks)
    } finally {
        extractor.release()
    }
}

/** 把 PCM 编码成 AAC */
private fun encodeAac(pcm: PcmData, durationUs: Long): EncodedTrack? {
    val format = MediaFormat.createAudioFormat(
        MediaFormat.MIMETYPE_AUDIO_AAC,
        pcm.sampleRate,
        pcm.channels
    ).apply {
        setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )
        setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE)
        setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_PCM_INPUT)
    }

    val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
    val samples = ArrayList<Sample>()
    val info = MediaCodec.BufferInfo()
    var outputFormat: MediaFormat? = null
    val bytesPerSecond = pcm.sampleRate * pcm.channels * 2

    try {
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        for (chunk in pcm.chunks) {
            if (chunk.presentationTimeUs > durationUs) break
            var offset = 0
            while (offset < chunk.data.size) {
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex < 0) {
                    outputFormat = drain(codec, samples, info) ?: outputFormat
                    continue
                }
                val buffer = codec.getInputBuffer(inputIndex)
                if (buffer == null) {
                    offset = chunk.data.size
                    continue
                }
                buffer.clear()
                val size = minOf(buffer.remaining(), chunk.data.size - offset)
                buffer.put(chunk.data, offset, size)
                val pts = chunk.presentationTimeUs + offset * 1_000_000L / bytesPerSecond
                codec.queueInputBuffer(inputIndex, 0, size, pts, 0)
                offset += size
                outputFormat = drain(codec, samples, info) ?: outputFormat
            }
        }

        val endIndex = codec.dequeueInputBuffer(20_000)
        if (endIndex >= 0) {
            codec.queueInputBuffer(
                endIndex,
                0,
                0,
                durationUs,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM
            )
        }
        outputFormat = drain(codec, samples, info, waitEndOfStream = true) ?: outputFormat
    } finally {
        runCatching { codec.stop() }
        codec.release()
    }

    val finalFormat = outputFormat ?: return null
    if (samples.isEmpty()) return null
    return EncodedTrack(finalFormat, samples)
}

// ---------------------------------------------------------------------------
// 合成
// ---------------------------------------------------------------------------

private fun mux(video: EncodedTrack, audio: EncodedTrack?, output: File) {
    if (output.exists()) output.delete()

    val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    try {
        val videoTrackIndex = muxer.addTrack(video.format)
        val audioTrackIndex = audio?.let { muxer.addTrack(it.format) } ?: -1
        muxer.start()

        var bufferSize = video.samples.maxOf { it.data.size }
        audio?.samples?.maxOfOrNull { it.data.size }?.let { bufferSize = maxOf(bufferSize, it) }
        val buffer = ByteBuffer.allocateDirect(maxOf(bufferSize, 1 shl 16))

        writeSamples(muxer, videoTrackIndex, video.samples, buffer, Long.MAX_VALUE)
        if (audio != null && audioTrackIndex >= 0) {
            writeSamples(muxer, audioTrackIndex, audio.samples, buffer, video.durationUs)
        }

        muxer.stop()
    } finally {
        runCatching { muxer.release() }
    }
}

private fun writeSamples(
    muxer: MediaMuxer,
    trackIndex: Int,
    samples: List<Sample>,
    buffer: ByteBuffer,
    maxPresentationTimeUs: Long
) {
    val info = MediaCodec.BufferInfo()
    for (sample in samples) {
        if (sample.presentationTimeUs > maxPresentationTimeUs) break
        if (sample.data.size > buffer.capacity()) continue
        buffer.clear()
        buffer.put(sample.data)
        buffer.position(0)
        buffer.limit(sample.data.size)
        info.set(0, sample.data.size, sample.presentationTimeUs, sample.flags)
        muxer.writeSampleData(trackIndex, buffer, info)
    }
}

// ---------------------------------------------------------------------------
// 编码器输出读取
// ---------------------------------------------------------------------------

private fun drain(
    codec: MediaCodec,
    samples: MutableList<Sample>,
    bufferInfo: MediaCodec.BufferInfo,
    waitEndOfStream: Boolean = false
): MediaFormat? {
    var format: MediaFormat? = null
    while (true) {
        val index = codec.dequeueOutputBuffer(
            bufferInfo,
            if (waitEndOfStream) 20_000 else 0
        )
        when {
            index == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                if (!waitEndOfStream) return format
            }

            index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                format = codec.outputFormat
            }

            index >= 0 -> {
                if (bufferInfo.size > 0) {
                    val buffer = codec.getOutputBuffer(index)
                    if (buffer != null) {
                        buffer.position(bufferInfo.offset)
                        buffer.limit(bufferInfo.offset + bufferInfo.size)
                        val data = ByteArray(bufferInfo.size)
                        buffer.get(data)
                        val isConfig = bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                        if (!isConfig) {
                            samples.add(
                                Sample(data, bufferInfo.presentationTimeUs, bufferInfo.flags)
                            )
                        }
                    }
                }
                codec.releaseOutputBuffer(index, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    return format
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 图像处理
// ---------------------------------------------------------------------------

private fun chooseVideoSize(reference: Bitmap): Pair<Int, Int> {
    val ratio = reference.width.toFloat() / reference.height.toFloat()
    var width = MAX_VIDEO_WIDTH
    var height = (width / ratio).toInt()
    if (height > MAX_VIDEO_HEIGHT) {
        height = MAX_VIDEO_HEIGHT
        width = (height * ratio).toInt()
    }
    width = (width / 2) * 2
    height = (height / 2) * 2
    return width.coerceAtLeast(2) to height.coerceAtLeast(2)
}

/** 等比缩放并补黑边，保证输出尺寸固定 */
private fun fitBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
    val target = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(target)
    canvas.drawColor(Color.BLACK)
    val scale = minOf(width.toFloat() / source.width, height.toFloat() / source.height)
    val drawWidth = source.width * scale
    val drawHeight = source.height * scale
    val left = (width - drawWidth) / 2f
    val top = (height - drawHeight) / 2f
    canvas.drawBitmap(
        source,
        null,
        RectF(left, top, left + drawWidth, top + drawHeight),
        Paint(Paint.FILTER_BITMAP_FLAG)
    )
    return target
}

private fun toYuvPlanes(bitmap: Bitmap, width: Int, height: Int): YuvPlanes {
    val scaled = fitBitmap(bitmap, width, height)
    val pixels = IntArray(width * height)
    scaled.getPixels(pixels, 0, width, 0, 0, width, height)
    scaled.recycle()

    val y = ByteArray(width * height)
    for (index in 0 until width * height) {
        val color = pixels[index]
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        y[index] = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).toByte()
    }

    val halfWidth = width / 2
    val halfHeight = height / 2
    val u = ByteArray(halfWidth * halfHeight)
    val v = ByteArray(halfWidth * halfHeight)
    for (row in 0 until halfHeight) {
        for (col in 0 until halfWidth) {
            var sumR = 0
            var sumG = 0
            var sumB = 0
            for (dy in 0..1) {
                for (dx in 0..1) {
                    val color = pixels[(row * 2 + dy) * width + (col * 2 + dx)]
                    sumR += (color shr 16) and 0xFF
                    sumG += (color shr 8) and 0xFF
                    sumB += color and 0xFF
                }
            }
            val r = sumR / 4
            val g = sumG / 4
            val b = sumB / 4
            val index = row * halfWidth + col
            u[index] = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128)
                .coerceIn(0, 255).toByte()
            v[index] = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128)
                .coerceIn(0, 255).toByte()
        }
    }
    return YuvPlanes(y, u, v)
}

private fun fillImage(image: Image, planes: YuvPlanes, width: Int, height: Int) {
    val yPlane = image.planes[0]
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]

    val yBuffer = yPlane.buffer
    for (row in 0 until height) {
        var position = row * yPlane.rowStride
        val rowStart = row * width
        for (col in 0 until width) {
            yBuffer.put(position, planes.y[rowStart + col])
            position += yPlane.pixelStride
        }
    }

    val halfWidth = width / 2
    val halfHeight = height / 2
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer
    for (row in 0 until halfHeight) {
        var uPosition = row * uPlane.rowStride
        var vPosition = row * vPlane.rowStride
        val rowStart = row * halfWidth
        for (col in 0 until halfWidth) {
            uBuffer.put(uPosition, planes.u[rowStart + col])
            vBuffer.put(vPosition, planes.v[rowStart + col])
            uPosition += uPlane.pixelStride
            vPosition += vPlane.pixelStride
        }
    }
}

// ---------------------------------------------------------------------------
// 网络
// ---------------------------------------------------------------------------

/** 只解码用于合成的图，必要时先降采样，避免大图占内存 */
private fun loadBitmapForVideo(url: String): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    try {
        openStream(url).use { BitmapFactory.decodeStream(it, null, bounds) }
    } catch (_: Exception) {
        return null
    }

    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= MAX_VIDEO_WIDTH) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }

    return try {
        openStream(url).use { BitmapFactory.decodeStream(it, null, options) }
    } catch (_: Exception) {
        null
    }
}

private fun openStream(url: String): InputStream {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 15_000
    connection.readTimeout = 30_000
    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
    if (connection.responseCode !in 200..299) {
        connection.disconnect()
        throw IOException("下载失败：HTTP ${connection.responseCode}")
    }
    return connection.inputStream
}

private fun download(url: String, file: File) {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        if (connection.responseCode !in 200..299) {
            throw IOException("下载背景音乐失败：HTTP ${connection.responseCode}")
        }
        connection.inputStream.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
    } finally {
        connection.disconnect()
    }
}

// ---------------------------------------------------------------------------
// 小数据结构
// ---------------------------------------------------------------------------

private const val FRAMES_PER_SECOND = 24
private const val MAX_VIDEO_WIDTH = 1080
private const val MAX_VIDEO_HEIGHT = 1920
private const val AUDIO_BIT_RATE = 128_000
private const val MAX_IMAGES = 9
private const val MAX_PCM_CHUNKS = 20_000
private const val MAX_PCM_INPUT = 65_536

private class Sample(
    val data: ByteArray,
    val presentationTimeUs: Long,
    val flags: Int
)

private class EncodedTrack(
    val format: MediaFormat,
    val samples: List<Sample>
) {
    val durationUs: Long
        get() = samples.lastOrNull()?.presentationTimeUs ?: 0L
}

private class PcmData(
    val sampleRate: Int,
    val channels: Int,
    val chunks: List<Sample>
)

private class YuvPlanes(
    val y: ByteArray,
    val u: ByteArray,
    val v: ByteArray
)
