package com.marchuck.latlngboundsfeature.offlineManager

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters


class OfflineMapWorker(val context: Context, parameters: WorkerParameters) : Worker(context, parameters) {

    override fun doWork(): Result {
        return Result.success()
    }
}