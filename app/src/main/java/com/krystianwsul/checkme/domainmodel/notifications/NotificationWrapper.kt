package com.krystianwsul.checkme.domainmodel.notifications

import android.os.Build
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.TimeStamp

abstract class NotificationWrapper {

    companion object {

        val instance = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 -> NotificationWrapperImplOMr1()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> NotificationWrapperImplO()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> NotificationWrapperImplN()
            else -> NotificationWrapperImpl()
        }
    }

    abstract fun cancelNotification(id: Int)

    abstract fun notifyInstance(deviceDbInfo: DeviceDbInfo, instance: Instance<*>, silent: Boolean, now: ExactTimeStamp)

    abstract fun notifyGroup(instances: Collection<Instance<*>>, silent: Boolean, now: ExactTimeStamp)

    abstract fun cleanGroup(lastNotificationId: Int?)

    abstract fun updateAlarm(nextAlarm: TimeStamp?)

    abstract fun logNotificationIds(source: String)

    abstract fun notifyTemporary(source: String)

    abstract fun hideTemporary(source: String)
}
