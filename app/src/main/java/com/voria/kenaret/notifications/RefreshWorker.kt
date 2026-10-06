package com.voria.kenaret.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.voria.kenaret.KenaretApp

/** Runs about once a day and extends the reminder plan further into the future. */
class RefreshWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? KenaretApp ?: return Result.success()
        app.container.scheduler.reschedule()
        return Result.success()
    }
}
