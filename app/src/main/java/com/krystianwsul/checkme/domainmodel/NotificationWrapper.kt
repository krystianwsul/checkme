package com.krystianwsul.checkme.domainmodel

import android.app.PendingIntent
import android.content.Context
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.utils.time.TimeStamp

abstract class NotificationWrapper {

    companion object {

        var instance: NotificationWrapper = NotificationWrapperImpl()
    }

    abstract fun cancelNotification(context: Context, id: Int)

    abstract fun notifyInstance(context: Context, domainFactory: DomainFactory, instance: Instance, silent: Boolean, now: ExactTimeStamp, nougat: Boolean)

    abstract fun notifyGroup(context: Context, domainFactory: DomainFactory, instances: Collection<Instance>, silent: Boolean, now: ExactTimeStamp, nougat: Boolean)

    abstract fun setAlarm(context: Context, pendingIntent: PendingIntent, nextAlarm: TimeStamp)

    abstract fun cleanGroup(context: Context, lastNotificationId: Int?)

    abstract fun getPendingIntent(context: Context): PendingIntent

    abstract fun cancelAlarm(context: Context, pendingIntent: PendingIntent)
}
