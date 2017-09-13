package com.krystianwsul.checkme.domainmodel

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.provider.Settings
import android.support.v4.app.NotificationCompat
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.notifications.TickService
import junit.framework.Assert

@SuppressLint("NewApi")
class NotificationWrapperImplO : NotificationWrapperImplN() {

    private val CHANNEL_ID = "channel"
    private val CHANNEL = NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH)

    private val SILENT_CHANNEL_ID = "silentChannel"
    private val SILENT_CHANNEL = NotificationChannel(SILENT_CHANNEL_ID, "Silent reminders", NotificationManager.IMPORTANCE_LOW)

    init {
        CHANNEL.enableVibration(true)

        notificationManager.createNotificationChannels(listOf(CHANNEL, SILENT_CHANNEL))
    }

    override fun notify(title: String, text: String?, notificationId: Int, deleteIntent: PendingIntent, contentIntent: PendingIntent, silent: Boolean, actions: List<NotificationCompat.Action>, time: Long?, style: NotificationCompat.Style?, autoCancel: Boolean, summary: Boolean, sortKey: String) {
        Assert.assertTrue(title.isNotEmpty())

        val builder = NotificationCompat.Builder(MyApplication.instance, if (silent) SILENT_CHANNEL_ID else CHANNEL_ID)
                .setContentTitle(title)
                .setSmallIcon(R.drawable.ikona_bez)
                .setDeleteIntent(deleteIntent)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSortKey(sortKey) as NotificationCompat.Builder

        if (!text.isNullOrEmpty())
            builder.setContentText(text)

        if (!silent)
            builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)

        Assert.assertTrue(actions.size <= 3)

        actions.forEach { builder.addAction(it) }

        if (time != null)
            builder.setWhen(time).setShowWhen(true)

        if (style != null)
            builder.setStyle(style)

        if (autoCancel)
            builder.setAutoCancel(true)

        builder.setGroup(TickService.GROUP_KEY)

        if (summary)
            builder.setGroupSummary(true)

        val notification = builder.build()

        @Suppress("Deprecation")
        if (!silent)
            notification.defaults = notification.defaults or Notification.DEFAULT_VIBRATE

        MyCrashlytics.log("NotificationManager.notify " + notificationId)
        notificationManager.notify(notificationId, notification)
    }
}