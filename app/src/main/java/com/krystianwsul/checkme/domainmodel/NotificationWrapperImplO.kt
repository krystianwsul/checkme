package com.krystianwsul.checkme.domainmodel

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.krystianwsul.checkme.MyApplication


@SuppressLint("NewApi")
open class NotificationWrapperImplO : NotificationWrapperImplN() {

    companion object {

        private const val CHANNEL_ID = "channel"
        private val CHANNEL = NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH)

        private const val SILENT_CHANNEL_ID = "silentChannel"
        private val SILENT_CHANNEL = NotificationChannel(SILENT_CHANNEL_ID, "Silent reminders", NotificationManager.IMPORTANCE_LOW)
    }

    init {
        CHANNEL.enableVibration(true)

        notificationManager.createNotificationChannels(listOf(CHANNEL, SILENT_CHANNEL))
    }

    override fun newBuilder(silent: Boolean) = NotificationCompat.Builder(MyApplication.instance, if (silent) SILENT_CHANNEL_ID else CHANNEL_ID)
}