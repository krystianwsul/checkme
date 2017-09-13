package com.krystianwsul.checkme.domainmodel

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager

@SuppressLint("NewApi")
class NotificationWrapperImplO : NotificationWrapperImplN() {

    private val CHANNEL_ID = "channel"
    private val CHANNEL = NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH)

    init {
        CHANNEL.enableVibration(true)

        notificationManager.createNotificationChannel(CHANNEL)
    }
}