package com.krystianwsul.checkme.domainmodel

import android.app.PendingIntent
import android.os.Build
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.utils.time.TimeStamp

abstract class NotificationWrapper {

    companion object {

        val instance: NotificationWrapper = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> NotificationWrapperImplO()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> NotificationWrapperImplM()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> NotificationWrapperImplN()
            else -> NotificationWrapperImpl()
        }
    }

    abstract fun cancelNotification(id: Int)

    abstract fun notifyInstance(domainFactory: DomainFactory, instance: Instance, silent: Boolean, now: ExactTimeStamp)

    abstract fun notifyGroup(domainFactory: DomainFactory, instances: Collection<Instance>, silent: Boolean, now: ExactTimeStamp)

    abstract fun cleanGroup(lastNotificationId: Int?)

    abstract fun updateAlarm(nextAlarm: TimeStamp?)

    protected abstract fun setExact(time: Long, pendingIntent: PendingIntent)
}
