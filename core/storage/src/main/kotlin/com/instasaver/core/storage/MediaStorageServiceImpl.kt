package com.instasaver.core.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.instasaver.core.model.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStorageServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val audioDemuxer: AudioDemuxer
) : MediaStorageService {

    override fun hasSufficientSpace(requiredBytes: Long): Boolean {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val available = stat.availableBlocksLong * stat.blockSizeLong
            available > requiredBytes
        } catch (e: Exception) {
            true
        }
    }

    override suspend fun saveMediaToGallery(
        downloadUrl: String,
        authorUsername: String,
        shortcode: String,
        mediaType: MediaType,
        onProgress: (Float, Long, Long) -> Unit
    ): Result<Uri> = withContext(Dispatchers.IO) {
        if (!hasSufficientSpace()) {
            return@withContext Result.failure(Exception("Insufficient phone storage. Please free up space."))
        }

        val request = Request.Builder().url(downloadUrl).build()
        val response = okHttpClient.newCall(request).execute()

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
                val tempCacheVideo = File(context.cacheDir, "temp_${System.currentTimeMillis()}_$shortcode.mp4")
                val tempCacheAudio = File(context.cacheDir, "temp_${System.currentTimeMillis()}_$shortcode.m4a")

                try {
                    FileOutputStream(tempCacheVideo).use { fos ->
                        body.byteStream().use { ins ->
                            copyStreamWithProgress(ins, fos, contentLength, onProgress)
                        }
                    }

                    val demuxSuccess = audioDemuxer.extractAudioTrackNative(tempCacheVideo, tempCacheAudio)
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
                resolver.openOutputStream(insertedUri)?.use { outputStream ->
                    body.byteStream().use { inputStream ->
                        copyStreamWithProgress(inputStream, outputStream, contentLength, onProgress)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(insertedUri, values, null, null)
            }

            Result.success(insertedUri)
        } catch (e: Exception) {
            resolver.delete(insertedUri, null, null)
            Result.failure(e)
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
