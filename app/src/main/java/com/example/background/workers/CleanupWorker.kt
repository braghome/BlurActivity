package com.example.background.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.background.OUTPUT_PATH
import timber.log.Timber
import java.io.File
import java.lang.Exception

class CleanupWorker(ctx: Context, parameters: WorkerParameters) : Worker(ctx, parameters) {
    override fun doWork(): Result {
        val appContext = applicationContext
        makeStatusNotification("Cleaning up old temporary files", appContext)
        return try {
            val fileDirectory = File(appContext.filesDir, OUTPUT_PATH)
            if (fileDirectory.exists()) {
                val listFiles = fileDirectory.listFiles()
                listFiles?.let {
                    for (file in it) {
                        val fileName = file.name
                        if (fileName.isNotBlank() && fileName.endsWith(".png")) {
                            val deleted = file.delete()
                            Timber.i("Deleted $fileName - $deleted")
                        }
                    }
                }
            }
            Result.success()
        } catch (exception: Exception) {
            Timber.e(exception.localizedMessage)
            Timber.e(exception.fillInStackTrace())
            Result.failure()
        }
    }
}