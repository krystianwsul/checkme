package com.krystianwsul.checkme.domainmodel.notifications

import android.os.Build
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.TimeStamp

abstract class NotificationWrapper {

    companion object {

        val instance = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> NotificationWrapperImplQ()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 -> NotificationWrapperImplOMr1()
            else -> NotificationWrapperImpl()
        }
    }

    abstract fun cancelNotification(id: Int, tag: String? = null)

    abstract fun notifyInstance(
        deviceDbInfo: DeviceDbInfo,
        instance: Instance,
        silent: Boolean,
        now: ExactTimeStamp.Local,
    )

    abstract fun notifyProject(
        project: SharedProject,
        instances: List<Instance>,
        timeStamp: TimeStamp,
        silent: Boolean,
        now: ExactTimeStamp.Local,
    )

    abstract fun notifyGroup(
        instances: Collection<Instance>,
        silent: Boolean,
        now: ExactTimeStamp.Local,
        summary: Boolean = true,
        projects: Collection<GroupTypeFactory.Notification.Project> = emptyList(),
    )

    abstract fun cleanGroup(lastNotificationId: Int?)

    abstract fun updateAlarm(nextAlarm: TimeStamp?)

    abstract fun notifyTemporary(notificationId: Int, source: String)

    abstract fun hideTemporary(notificationId: Int, source: String)
}
