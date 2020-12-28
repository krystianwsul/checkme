package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactory.Companion.syncOnDomain
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.EditInstancesViewModel
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.InstanceKey

fun DomainFactory.getEditInstancesData(instanceKeys: List<InstanceKey>): EditInstancesViewModel.Data = syncOnDomain {
    MyCrashlytics.log("DomainFactory.getEditInstancesData")

    check(instanceKeys.isNotEmpty())

    val now = ExactTimeStamp.Local.now

    val currentCustomTimes = getCurrentRemoteCustomTimes(now).associateBy {
        it.key
    }.toMutableMap<CustomTimeKey<*>, Time.Custom<*>>()

    val instanceDatas = mutableMapOf<InstanceKey, EditInstancesViewModel.InstanceData>()

    for (instanceKey in instanceKeys) {
        val instance = getInstance(instanceKey)
        check(instance.isRootInstance())
        check(instance.done == null)

        instanceDatas[instanceKey] = EditInstancesViewModel.InstanceData(
                instance.instanceDateTime,
                instance.name,
                instance.done != null
        )

        (instance.instanceTime as? Time.Custom<*>)?.let {
            currentCustomTimes[it.key] = it
        }
    }

    val customTimeDatas = currentCustomTimes.mapValues {
        it.value.let {
            EditInstancesViewModel.CustomTimeData(
                    it.key,
                    it.name,
                    it.hourMinutes.toSortedMap()
            )
        }
    }

    val showHour = instanceDatas.values.all {
        it.instanceDateTime.timeStamp.toLocalExactTimeStamp() < now
    }

    EditInstancesViewModel.Data(instanceDatas, customTimeDatas, showHour)
}

fun DomainFactory.setInstancesDateTime(
        dataId: Int,
        source: SaveService.Source,
        instanceKeys: Set<InstanceKey>,
        instanceDate: Date,
        instanceTimePair: TimePair
): DomainFactory.EditInstancesUndoData = syncOnDomain {
    MyCrashlytics.log("DomainFactory.setInstancesDateTime")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    check(instanceKeys.isNotEmpty())

    val now = ExactTimeStamp.Local.now

    val instances = instanceKeys.map(this::getInstance)

    val editInstancesUndoData = DomainFactory.EditInstancesUndoData(
            instances.map { it.instanceKey to it.instanceDateTime }
    )

    instances.forEach {
        it.setInstanceDateTime(
                localFactory,
                ownerKey,
                DateTime(instanceDate, getTime(instanceTimePair)),
                now
        )
    }

    val projects = instances.map { it.task.project }.toSet()

    updateNotifications(now)

    save(dataId, source)

    notifyCloud(projects)

    editInstancesUndoData
}