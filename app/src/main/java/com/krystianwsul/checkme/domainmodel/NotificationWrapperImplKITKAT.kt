package com.krystianwsul.checkme.domainmodel

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import junit.framework.Assert

open class NotificationWrapperImplKITKAT : NotificationWrapperImpl() {

    @SuppressLint("NewApi")
    override fun setExact(time: Long, pendingIntent: PendingIntent) {
        Assert.assertTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        Assert.assertTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
    }
}