package com.krystianwsul.checkme.domainmodel

import android.app.PendingIntent
import android.os.Build
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.utils.time.TimeStamp

abstract class NotificationWrapper {

    companion object {

        var instance: NotificationWrapper = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> NotificationWrapperImplO()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> NotificationWrapperImplN()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> NotificationWrapperImplM()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> NotificationWrapperImplKITKAT()
            else -> NotificationWrapperImpl()
        }
    }

    abstract fun cancelNotification(id: Int)

    abstract fun notifyInstance(domainFactory: DomainFactory, instance: Instance, silent: Boolean, now: ExactTimeStamp)

    abstract fun notifyGroup(domainFactory: DomainFactory, instances: Collection<Instance>, silent: Boolean, now: ExactTimeStamp)

    abstract fun setAlarm(pendingIntent: PendingIntent, nextAlarm: TimeStamp)

    abstract fun cleanGroup(lastNotificationId: Int?)

    abstract fun getPendingIntent(): PendingIntent

    abstract fun cancelAlarm(pendingIntent: PendingIntent)
}
