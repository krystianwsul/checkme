package com.krystianwsul.checkme.domainmodel

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import junit.framework.Assert

open class NotificationWrapperImplM : NotificationWrapperImplKITKAT() {

    @SuppressLint("NewApi")
    override fun setExact(context: Context, time: Long, pendingIntent: PendingIntent) {
        Assert.assertTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
    }
}