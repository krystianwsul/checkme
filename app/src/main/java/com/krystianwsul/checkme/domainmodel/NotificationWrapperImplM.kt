package com.krystianwsul.checkme.domainmodel

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent


@SuppressLint("NewApi")
open class NotificationWrapperImplM : NotificationWrapperImpl() {

    override fun setExact(time: Long, pendingIntent: PendingIntent) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
    }
}