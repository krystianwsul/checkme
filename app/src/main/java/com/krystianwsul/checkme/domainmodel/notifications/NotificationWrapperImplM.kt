package com.krystianwsul.checkme.domainmodel.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.util.Log


@SuppressLint("NewApi")
open class NotificationWrapperImplM : NotificationWrapperImpl() {

    override fun setExact(time: Long, pendingIntent: PendingIntent) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
    }

    override fun unchanged(notificationHash: NotificationHash) = notificationManager.activeNotifications
            ?.singleOrNull { it.id == notificationHash.id }
            ?.let {
                Log.e("asdf", "hash codes: " + it.notification.extras.getInt(KEY_HASH_CODE) + " - " + notificationHash.hashCode())
                it.notification.extras.getInt(KEY_HASH_CODE) == notificationHash.hashCode()
            }
            ?: false
}