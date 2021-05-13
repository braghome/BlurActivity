package com.example.background.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.KEY_IMAGE_URI
import com.example.background.R
import timber.log.Timber

class BlurWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val appContext = applicationContext
        val resourceUri = inputData.getString(KEY_IMAGE_URI)
        makeStatusNotification("Blurring image", appContext)
        sleep()
        return try {
            if (TextUtils.isEmpty(resourceUri)) {
                Timber.e("Invalid input uri")
                throw IllegalAccessException("Invalid input uri")
            }
            val contentResolver = appContext.contentResolver
            val picture = BitmapFactory.decodeStream(
                 contentResolver.openInputStream(Uri.parse(resourceUri)))
            val blurBitmap = blurBitmap(picture, appContext)
            val bitmapUri = writeBitmapToFile(appContext, blurBitmap)
            val outputData = workDataOf(KEY_IMAGE_URI to bitmapUri.toString())
            makeStatusNotification("Output is $bitmapUri", appContext)
            Result.success(outputData)
        } catch (throwable: Throwable) {
            Timber.e("error applying Blur")
            Result.failure()
        }
    }
}