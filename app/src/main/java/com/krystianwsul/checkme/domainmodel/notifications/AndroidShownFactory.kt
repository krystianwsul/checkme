package com.krystianwsul.checkme.domainmodel.notifications

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.TaskKeyData

class AndroidShownFactory(private val notificationStorage: NotificationStorage) : Instance.ShownFactory {

    override fun createShown(taskKeyData: TaskKeyData, scheduleDateTime: DateTime): Instance.Shown {
        val instanceShownKey = InstanceShownKey(taskKeyData, scheduleDateTime)

        check(!notificationStorage.instanceShownMap.containsKey(instanceShownKey))

        notificationStorage.instanceShownMap[instanceShownKey] = InstanceShownData()

        return Shown(instanceShownKey)
    }

    override fun getShown(taskKey: TaskKey, scheduleDateTime: DateTime): Instance.Shown? {
        val instanceShownKey = InstanceShownKey(TaskKeyData(taskKey), scheduleDateTime)

        return if (notificationStorage.instanceShownMap.containsKey(instanceShownKey))
            Shown(instanceShownKey)
        else
            null
    }

    inner class Shown(private val instanceShownKey: InstanceShownKey) : Instance.Shown {

        private var instanceShownData
            get() = notificationStorage.instanceShownMap.getValue(instanceShownKey)
            set(value) {
                notificationStorage.instanceShownMap[instanceShownKey] = value
            }

        override var notified: Boolean
            get() = instanceShownData.notified
            set(value) {
                instanceShownData = instanceShownData.copy(notified = value)
            }

        override var notificationShown: Boolean
            get() = instanceShownData.notificationShown
            set(value) {
                instanceShownData = instanceShownData.copy(notificationShown = value)
            }
    }
}