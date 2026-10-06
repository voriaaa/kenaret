package com.voria.kenaret.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.voria.kenaret.MainActivity
import com.voria.kenaret.R

object NotificationHelper {
    const val CHANNEL_REMINDERS = "kenaret_reminders"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }
        manager.createNotificationChannel(channel)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Shows a reminder. The lock-screen ("public") version is always neutral so nothing sensitive is
     * visible on a locked phone, regardless of the private-notifications setting.
     */
    @SuppressLint("MissingPermission")
    fun show(context: Context, id: Int, title: String, body: String) {
        if (!hasPermission(context)) return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val neutralTitle = context.getString(R.string.app_name)
        val neutralBody = context.getString(R.string.notification_private_body)
        val color = ContextCompat.getColor(context, R.color.brand_primary)

        val publicVersion = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(color)
            .setContentTitle(neutralTitle)
            .setContentText(neutralBody)
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(color)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        manager.notify(id, notification)
    }

    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
