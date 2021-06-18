package com.krystianwsul.checkme.domainmodel.notifications

import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.utils.InstanceKey

class AndroidShownFactory(private val notificationStorage: FactoryProvider.NotificationStorage) : Instance.ShownFactory {

    override val instanceShownMap: Map<InstanceKey, Instance.Shown>
        get() = notificationStorage.instanceShownMap.mapValues { Shown(it.key) }

    override fun createShown(instanceKey: InstanceKey): Instance.Shown {
        check(!notificationStorage.instanceShownMap.containsKey(instanceKey))

        notificationStorage.instanceShownMap[instanceKey] = InstanceShownData()

        return Shown(instanceKey)
    }

    override fun getShown(instanceKey: InstanceKey): Instance.Shown? {
        return if (notificationStorage.instanceShownMap.containsKey(instanceKey))
            Shown(instanceKey)
        else
            null
    }

    inner class Shown(override val instanceKey: InstanceKey) : Instance.Shown {

        private var instanceShownData
            get() = notificationStorage.instanceShownMap.getValue(instanceKey)
            set(value) {
                notificationStorage.instanceShownMap[instanceKey] = value
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

        override fun delete() {
            notificationStorage.instanceShownMap.remove(instanceKey)
        }
    }
}