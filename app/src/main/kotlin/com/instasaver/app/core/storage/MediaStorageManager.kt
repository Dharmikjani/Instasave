package com.instasaver.app.core.storage

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.instasaver.app.core.model.MediaType
import com.instasaver.app.core.network.InstagramHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MediaStorageManager {

    /**
     * Checks if available disk space is greater than required minimum (default 20MB).
     */
    fun hasSufficientSpace(context: Context, requiredBytes: Long = 20 * 1024 * 1024): Boolean {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val available = stat.availableBlocksLong * stat.blockSizeLong
            available > requiredBytes
        } catch (e: Exception) {
            true // fallback to allow attempt
        }
    }

    /**
     * Downloads media directly to Android MediaStore Gallery with IS_PENDING safety,
     * native lossless audio demuxing, and strict completion validation.
     */
    suspend fun saveMediaToGallery(
        context: Context,
        downloadUrl: String,
        authorUsername: String,
        shortcode: String,
        mediaType: MediaType,
        onProgress: (Float, Long, Long) -> Unit = { _, _, _ -> }
    ): Result<Uri> = withContext(Dispatchers.IO) {
        if (!hasSufficientSpace(context)) {
            return@withContext Result.failure(Exception("Insufficient phone storage. Please free up space."))
        }

        val request = Request.Builder().url(downloadUrl).build()
        val response = InstagramHttpClient.client.newCall(request).execute()

        if (!response.isSuccessful) {
            return@withContext Result.failure(Exception("HTTP error ${response.code} downloading media."))
        }

        val body = response.body ?: return@withContext Result.failure(Exception("Empty response body from media server."))
        val contentLength = body.contentLength()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cleanAuthor = authorUsername.replace(Regex("[^a-zA-Z0-9_]"), "")
        val extension = when (mediaType) {
            MediaType.REEL, MediaType.POST_VIDEO -> "mp4"
            MediaType.AUDIO -> "m4a"
            else -> "jpg"
        }
        val uniqueSuffix = (1000..9999).random()
        val fileName = "Insta_${cleanAuthor}_${timestamp}_${shortcode}_$uniqueSuffix.$extension"

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(mediaType))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
                val relativePath = when (mediaType) {
                    MediaType.REEL, MediaType.POST_VIDEO -> Environment.DIRECTORY_MOVIES + "/InstaSaver/$cleanAuthor"
                    MediaType.AUDIO -> Environment.DIRECTORY_MUSIC + "/InstaSaver"
                    else -> Environment.DIRECTORY_PICTURES + "/InstaSaver/$cleanAuthor"
                }
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }
        }

        val targetCollection = when (mediaType) {
            MediaType.REEL, MediaType.POST_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            MediaType.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val insertedUri = resolver.insert(targetCollection, values)
            ?: return@withContext Result.failure(Exception("Could not create MediaStore entry."))

        try {
            if (mediaType == MediaType.AUDIO) {
                // True Lossless Audio Demuxing: Extract pure AAC audio track from MP4 container into .m4a
                val tempCacheVideo = File(context.cacheDir, "temp_${System.currentTimeMillis()}_$shortcode.mp4")
                val tempCacheAudio = File(context.cacheDir, "temp_${System.currentTimeMillis()}_$shortcode.m4a")

                try {
                    FileOutputStream(tempCacheVideo).use { fos ->
                        body.byteStream().use { ins ->
                            copyStreamWithProgress(ins, fos, contentLength, onProgress)
                        }
                    }

                    val demuxSuccess = extractAudioTrackNative(tempCacheVideo, tempCacheAudio)
                    val sourceAudioFile = if (demuxSuccess && tempCacheAudio.exists() && tempCacheAudio.length() > 0) {
                        tempCacheAudio
                    } else {
                        tempCacheVideo
                    }

                    resolver.openOutputStream(insertedUri)?.use { outStream ->
                        sourceAudioFile.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                } finally {
                    tempCacheVideo.delete()
                    tempCacheAudio.delete()
                }
            } else {
                // Direct stream copy for 1080p Videos and Photos
                resolver.openOutputStream(insertedUri)?.use { outputStream ->
                    body.byteStream().use { inputStream ->
                        copyStreamWithProgress(inputStream, outputStream, contentLength, onProgress)
                    }
                }
            }

            // Release IS_PENDING so it becomes visible in the gallery
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(insertedUri, values, null, null)
            }

            Result.success(insertedUri)
        } catch (e: Exception) {
            // Clean up partial corrupted file on failure
            resolver.delete(insertedUri, null, null)
            Result.failure(e)
        }
    }

    /**
     * Losslessly extracts the audio stream from an MP4 video into an MPEG-4 (.m4a) container
     * using native Android SDK MediaExtractor and MediaMuxer without re-encoding.
     */
    private fun extractAudioTrackNative(sourceFile: File, destM4aFile: File): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return try {
            extractor.setDataSource(sourceFile.absolutePath)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                return false
            }

            extractor.selectTrack(audioTrackIndex)
            muxer = MediaMuxer(destM4aFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val maxBufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                64 * 1024
            }
            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                extractor.advance()
            }
            true
        } catch (e: Exception) {
            false
        } finally {
            try { extractor.release() } catch (e: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {}
        }
    }

    private fun copyStreamWithProgress(
        input: InputStream,
        output: OutputStream,
        totalBytes: Long,
        onProgress: (Float, Long, Long) -> Unit
    ) {
        val buffer = ByteArray(16 * 1024)
        var bytesCopied = 0L
        var read: Int

        while (input.read(buffer).also { read = it } != -1) {
            output.write(buffer, 0, read)
            bytesCopied += read
            val progress = if (totalBytes > 0) bytesCopied.toFloat() / totalBytes else -1f
            onProgress(progress, bytesCopied, totalBytes)
        }
        output.flush()

        // Strict data integrity validation
        if (bytesCopied == 0L) {
            throw java.io.IOException("Download failed: 0 bytes received from media server.")
        }
        if (totalBytes > 0 && bytesCopied < totalBytes) {
            throw java.io.IOException("Download incomplete: received $bytesCopied bytes of $totalBytes expected.")
        }
    }

    private fun getMimeType(mediaType: MediaType): String {
        return when (mediaType) {
            MediaType.REEL, MediaType.POST_VIDEO -> "video/mp4"
            MediaType.AUDIO -> "audio/mp4"
            else -> "image/jpeg"
        }
    }
}

