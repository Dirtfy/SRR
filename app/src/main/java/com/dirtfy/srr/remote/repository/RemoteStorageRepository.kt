package com.dirtfy.srr.remote.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.dirtfy.srr.core.repository.StorageRepository
import com.dirtfy.srr.core.util.planResize
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

class RemoteStorageRepository(private val context: Context) : StorageRepository {

    companion object {
        private const val MAX_DIMENSION = 1920
        private const val JPEG_QUALITY  = 85
    }

    override suspend fun uploadItemImage(uri: Uri): Result<String> = runCatching {
        val bytes = withContext(Dispatchers.IO) { encodeAsJpeg(uri) }
        val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
        val ref = Firebase.storage.reference.child("items/${UUID.randomUUID()}.jpg")
        ref.putBytes(bytes, metadata).await()
        ref.downloadUrl.await().toString()
    }

    override suspend fun deleteImage(url: String): Result<Unit> = runCatching {
        Firebase.storage.getReferenceFromUrl(url).delete().await()
    }

    /**
     * Reads [uri], applies EXIF rotation, resizes using [planResize] (pure Kotlin logic),
     * and compresses to JPEG. All Android bitmap I/O is contained here.
     */
    private fun encodeAsJpeg(uri: Uri): ByteArray {
        val cr = context.contentResolver

        // 1. Read EXIF orientation (independent stream — must open before decoding)
        val rotation = cr.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90  -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else                                 -> 0f
            }
        } ?: 0f

        // 2. Bounds-only pass to get pixel dimensions without allocating a bitmap
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }

        // 3. Delegate dimension math to pure Kotlin core logic
        val plan = planResize(boundsOpts.outWidth, boundsOpts.outHeight, MAX_DIMENSION)

        // 4. Decode at sub-sample size
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = plan.sampleSize }
        var bitmap = cr.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
            ?: throw IllegalStateException("Cannot decode image from URI")

        // 5. Apply EXIF rotation
        if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            bitmap = rotated
        }

        // 6. Scale to exact target dimensions computed by planResize
        if (bitmap.width != plan.targetWidth || bitmap.height != plan.targetHeight) {
            val scaled = Bitmap.createScaledBitmap(bitmap, plan.targetWidth, plan.targetHeight, true)
            bitmap.recycle()
            bitmap = scaled
        }

        // 7. Compress and return bytes
        return ByteArrayOutputStream().also { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            bitmap.recycle()
        }.toByteArray()
    }
}
