package com.example.background.workers

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
import android.provider.MediaStore.MediaColumns.*
import android.provider.MediaStore.VOLUME_EXTERNAL
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
            val insertImage = Images.Media.insertImage(resolver, bitmap, title,
                dateFormatter.format(Date()))
            if (insertImage.isNullOrEmpty().not()) {
                val output = workDataOf(KEY_IMAGE_URI to insertImage)
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
}