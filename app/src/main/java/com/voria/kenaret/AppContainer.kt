package com.voria.kenaret

import android.content.Context
import com.voria.kenaret.data.KenaretRepository
import com.voria.kenaret.database.KenaretDatabase
import com.voria.kenaret.notifications.ReminderScheduler

/** Manual dependency container (small app, no DI framework needed). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: KenaretDatabase = KenaretDatabase.create(appContext)
    val dao = database.dao()
    val scheduler = ReminderScheduler(appContext, dao)
    val repository = KenaretRepository(appContext, database, dao, scheduler)
}
