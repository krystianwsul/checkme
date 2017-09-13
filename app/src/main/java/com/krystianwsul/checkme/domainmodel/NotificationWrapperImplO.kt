package com.krystianwsul.checkme.domainmodel

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.support.v4.app.NotificationCompat
import com.krystianwsul.checkme.MyApplication

@SuppressLint("NewApi")
class NotificationWrapperImplO : NotificationWrapperImplN() {

    private val CHANNEL_ID = "channel"
    private val CHANNEL = NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH)

    init {
        CHANNEL.enableVibration(true)

        notificationManager.createNotificationChannel(CHANNEL)
    }

    override fun newNotificationBuilder() = NotificationCompat.Builder(MyApplication.instance, CHANNEL_ID)

    override fun setVibration(notification: Notification) = Unit
}