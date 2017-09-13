package com.krystianwsul.checkme.domainmodel

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.utils.time.TimeStamp

abstract class NotificationWrapper {

    companion object {

        var instance: NotificationWrapper = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> NotificationWrapperImplN()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> NotificationWrapperImplM()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> NotificationWrapperImplKITKAT()
            else -> NotificationWrapperImpl()
        }
    }

    abstract fun cancelNotification(context: Context, id: Int)

    abstract fun notifyInstance(context: Context, domainFactory: DomainFactory, instance: Instance, silent: Boolean, now: ExactTimeStamp)

    abstract fun notifyGroup(context: Context, domainFactory: DomainFactory, instances: Collection<Instance>, silent: Boolean, now: ExactTimeStamp)

    abstract fun setAlarm(context: Context, pendingIntent: PendingIntent, nextAlarm: TimeStamp)

    abstract fun cleanGroup(context: Context, lastNotificationId: Int?)

    abstract fun getPendingIntent(context: Context): PendingIntent

    abstract fun cancelAlarm(context: Context, pendingIntent: PendingIntent)
}
