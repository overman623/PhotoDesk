package com.happyhouse.photo.gallery

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException

data class Album(
    val bucketId: String,
    val name: String,
    val count: Int,
    val coverUri: Uri
)

data class Photo(
    val id: Long,
    val uri: Uri,
    val displayName: String
)

class GalleryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    fun loadAlbums(): List<Album> {
        val albums = linkedMapOf<String, AlbumBuilder>()
        queryImages(null, null).forEach { photo ->
            val bucketId = photo.bucketId ?: return@forEach
            val builder = albums.getOrPut(bucketId) {
                AlbumBuilder(bucketId, photo.bucketName ?: bucketId, photo.uri)
            }
            builder.count += 1
        }
        return albums.values
            .map { Album(it.bucketId, it.name, it.count, it.coverUri) }
            .sortedBy { it.name.lowercase() }
    }

    fun loadPhotos(bucketId: String): List<Photo> {
        return queryImages(
            "${MediaStore.Images.Media.BUCKET_ID}=?",
            arrayOf(bucketId)
        ).map { Photo(it.id, it.uri, it.displayName) }
    }

    fun loadAllPhotos(): List<Photo> {
        return queryImages(null, null).map { Photo(it.id, it.uri, it.displayName) }
    }

    fun movePhoto(photo: Photo, albumName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/$albumName/"
                )
            }
            val updated = resolver.update(photo.uri, values, null, null)
            if (updated == 0) {
                throw IOException("Could not move photo")
            }
            return
        }
        moveWithFileApi(photo, albumName)
    }

    private fun moveWithFileApi(photo: Photo, albumName: String) {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        var sourcePath: String? = null
        resolver.query(photo.uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                sourcePath = cursor.getString(0)
            }
        }
        val source = sourcePath?.let { File(it) } ?: throw IOException("Missing photo path")
        val destDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            albumName
        )
        if (!destDir.exists() && !destDir.mkdirs()) {
            throw IOException("Could not create album")
        }
        val dest = File(destDir, source.name)
        if (!source.renameTo(dest)) {
            source.inputStream().use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            if (!source.delete()) {
                throw IOException("Could not move photo")
            }
        }
        android.media.MediaScannerConnection.scanFile(
            appContext,
            arrayOf(dest.absolutePath),
            null,
            null
        )
    }

    private fun queryImages(selection: String?, args: Array<String>?): List<ImageRow> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED
        )
        val rows = mutableListOf<ImageRow>()
        resolver.query(
            collection,
            projection,
            selection,
            args,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                rows.add(
                    ImageRow(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id),
                        bucketId = cursor.getString(bucketCol),
                        bucketName = cursor.getString(bucketNameCol),
                        displayName = cursor.getString(nameCol) ?: ""
                    )
                )
            }
        }
        return rows
    }

    private data class ImageRow(
        val id: Long,
        val uri: Uri,
        val bucketId: String?,
        val bucketName: String?,
        val displayName: String
    )

    private data class AlbumBuilder(
        val bucketId: String,
        val name: String,
        val coverUri: Uri,
        var count: Int = 0
    )
}
