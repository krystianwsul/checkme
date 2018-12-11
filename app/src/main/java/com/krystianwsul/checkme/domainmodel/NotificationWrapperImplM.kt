package com.krystianwsul.checkme.domainmodel

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.notifications.AlarmReceiver


@SuppressLint("NewApi")
open class NotificationWrapperImplM : NotificationWrapperImpl() {

    override fun setExact(time: Long) {
        super.setExact(time)

        val pendingIntent = PendingIntent.getBroadcast(MyApplication.instance, 1, AlarmReceiver.newIntent("setIdle"), PendingIntent.FLAG_UPDATE_CURRENT)!!
        alarmManager.cancel(pendingIntent)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
    }
}