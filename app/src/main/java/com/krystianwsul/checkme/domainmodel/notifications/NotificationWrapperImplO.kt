package com.krystianwsul.checkme.domainmodel.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.krystianwsul.checkme.MyApplication


@SuppressLint("NewApi")
open class NotificationWrapperImplO : NotificationWrapperImpl() {

    companion object {

        private const val HIGH_CHANNEL_ID = "channel"

        private val HIGH_CHANNEL = NotificationChannel(
            HIGH_CHANNEL_ID,
            "Heads-up reminders",
            NotificationManager.IMPORTANCE_HIGH,
        )

        private const val MEDIUM_CHANNEL_ID = "mediumChannel"

        private val MEDIUM_CHANNEL = NotificationChannel(
                MEDIUM_CHANNEL_ID,
                "Regular reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
        )

        private const val SILENT_CHANNEL_ID = "silentChannel"

        private val SILENT_CHANNEL = NotificationChannel(
                SILENT_CHANNEL_ID,
                "Silent reminders",
                NotificationManager.IMPORTANCE_LOW,
        )
    }

    init {
        HIGH_CHANNEL.enableVibration(true)
        MEDIUM_CHANNEL.enableVibration(true)

        notificationManager.createNotificationChannels(listOf(HIGH_CHANNEL, MEDIUM_CHANNEL, SILENT_CHANNEL))
    }

    override fun newBuilder(silent: Boolean, highPriority: Boolean) = NotificationCompat.Builder(
            MyApplication.instance,
            when {
                silent -> SILENT_CHANNEL_ID
                highPriority -> HIGH_CHANNEL_ID
                else -> MEDIUM_CHANNEL_ID
            },
    )
}