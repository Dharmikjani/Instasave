package com.instasaver.app.core.gallery

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LocalSavedItem(
    val id: Long,
    val title: String,
    val uri: Uri,
    val isVideo: Boolean,
    val creatorName: String,
    val sizeBytes: Long,
    val dateAdded: Long
)

object GalleryScanner {

    suspend fun loadSavedMedia(context: Context): List<LocalSavedItem> = withContext(Dispatchers.IO) {
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
                val date = cursor.getLong(dateCol)
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
                        dateAdded = date
                    )
                )
            }
        }

        // 2. Scan Photos
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED
        )
        val imageSelection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val imageArgs = arrayOf("Insta_%")

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            imageProjection,
            imageSelection,
            imageArgs,
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
                val date = cursor.getLong(dateCol)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val creator = extractCreator(name)

                list.add(
                    LocalSavedItem(
                        id = id,
                        title = name,
                        uri = uri,
                        isVideo = false,
                        creatorName = creator,
                        sizeBytes = size,
                        dateAdded = date
                    )
                )
            }
        }

        list.sortedByDescending { it.dateAdded }
    }

    private fun extractCreator(fileName: String): String {
        val parts = fileName.split("_")
        return if (parts.size >= 2) parts[1] else "General"
    }
}

