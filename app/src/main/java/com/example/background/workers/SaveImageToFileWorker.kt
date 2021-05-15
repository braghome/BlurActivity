package com.example.background.workers

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment.*
import android.provider.MediaStore.Downloads.RELATIVE_PATH
import android.provider.MediaStore.Images.Media.*
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.KEY_IMAGE_URI
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class SaveImageToFileWorker(ctx: Context, parameters: WorkerParameters) : Worker(ctx, parameters) {

    private val title = "Blurred Image"
    private val dateFormatter = SimpleDateFormat(
        "yyyy.MM.dd 'at' HH:mm:ss z", Locale.getDefault()
    )
    private val legacyOrQ: (Bitmap) -> Uri = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        saveImageInQ(it) else legacySave(it) }

    override fun doWork(): Result {
        val appContext = applicationContext
        val resourceUri = inputData.getString(KEY_IMAGE_URI)
        val resolver = appContext.contentResolver
        makeStatusNotification("Saving image", appContext)
        sleep()
        return try {
            if (TextUtils.isEmpty(resourceUri)) {
                Timber.e("Invalid input uri")
                throw IllegalAccessException("Invalid input uri")
            }
            val bitmap = BitmapFactory.decodeStream(
                resolver.openInputStream(Uri.parse(resourceUri)))

            val uri = legacyOrQ(bitmap)
            val saveLocation = uri.toString()
            if (saveLocation.isEmpty().not()) {
                val output = workDataOf(KEY_IMAGE_URI to saveLocation)
                Result.success(output)
            } else {
                Timber.e("Writing to MediaStore failed")
                Result.failure()
            }
        } catch (exception: Exception) {
            Timber.e(exception.localizedMessage)
            Timber.e(exception.fillInStackTrace())
            Result.failure()
        }
    }

    private fun saveImageInQ(bitmap: Bitmap): Uri {
        val filename = "${title}_of_${dateFormatter.format(Date())}.png"
        val fos: OutputStream?
        val contentValues = ContentValues().apply {
            put(DISPLAY_NAME, filename)
            put(MIME_TYPE, "image/png")
            put(RELATIVE_PATH, DIRECTORY_DCIM)
            put(IS_PENDING, 1)
        }

        //use application context to get contentResolver
        val contentResolver = applicationContext.contentResolver
        val uri = contentResolver.insert(EXTERNAL_CONTENT_URI, contentValues)
        uri?.let { contentResolver.openOutputStream(it) }.also { fos = it }
        fos?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        fos?.flush()
        fos?.close()

        contentValues.clear()
        contentValues.put(IS_PENDING, 0)
        uri?.let {
            contentResolver.update(it, contentValues, null, null)
        }
        return uri!!
    }

    private fun legacySave(bitmap: Bitmap): Uri {
        val appContext = applicationContext
        val filename = "${title}_of_${dateFormatter.format(Date())}.png"
        val directory = getExternalStoragePublicDirectory(DIRECTORY_PICTURES)
        val file = File(directory, filename)
        val outStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        outStream.flush()
        outStream.close()
        MediaScannerConnection.scanFile(appContext, arrayOf(file.absolutePath),
            null, null)
        return FileProvider.getUriForFile(appContext, "${appContext.packageName}.provider",
            file)
    }
}