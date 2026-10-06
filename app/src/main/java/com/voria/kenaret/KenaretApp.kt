package com.voria.kenaret

import android.app.Application
import com.voria.kenaret.notifications.NotificationHelper
import com.voria.kenaret.settings.LocaleManager

class KenaretApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannels(LocaleManager.wrap(this))
    }
}
