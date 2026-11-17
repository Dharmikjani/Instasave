package com.instasaver.feature.gallery.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.instasaver.core.database.DatabaseRepository
import com.instasaver.core.model.LocalSavedItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databaseRepository: DatabaseRepository
) {

    suspend fun loadSavedMedia(): List<LocalSavedItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<LocalSavedItem>()

        // 1. Scan Videos
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )
        val videoSelection = "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
        val videoArgs = arrayOf("Insta_%")

        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                videoSelection,
                videoArgs,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol)
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val creator = extractCreator(name)

                    list.add(
                        LocalSavedItem(
                            id = id,
                            title = name,
                            uri = uri,
                            isVideo = true,
                            creatorName = creator,
                            sizeBytes = size,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        // 2. Scan Images
        val imgProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED
        )
        val imgSelection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val imgArgs = arrayOf("Insta_%")

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imgProjection,
                imgSelection,
                imgArgs,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol)
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val creator = extractCreator(name)

                    list.add(
                        LocalSavedItem(
                            id = id,
                            title = name,
                            uri = uri,
                            isVideo = false,
                            isAudio = false,
                            creatorName = creator,
                            sizeBytes = size,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        // 3. Scan Audio
        val audioProjection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED
        )
        val audioSelection = "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?"
        val audioArgs = arrayOf("Insta_%")

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                audioProjection,
                audioSelection,
                audioArgs,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol)
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val creator = extractCreator(name)

                    list.add(
                        LocalSavedItem(
                            id = id,
                            title = name,
                            uri = uri,
                            isVideo = false,
                            isAudio = true,
                            creatorName = creator,
                            sizeBytes = size,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        list.sortedByDescending { it.dateAdded }
    }

    private val creatorRegex = Regex("^Insta_(.+?)_\\d{8}_\\d{6}_")

    private fun extractCreator(filename: String): String {
        val match = creatorRegex.find(filename)
        if (match != null && match.groupValues.size > 1) {
            return match.groupValues[1]
        }
        val parts = filename.removePrefix("Insta_").split("_")
        return if (parts.isNotEmpty()) parts[0] else "unknown"
    }
}
