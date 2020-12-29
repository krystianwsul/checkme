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

    val instances = instanceKeys.map(::getInstance)
    check(instances.all { it.done == null })

    val parentInstanceData = instances.mapNotNull { it.parentInstanceData?.instance }
            .groupBy { it }
            .map { it.key to it.value.size }
            .maxByOrNull { it.second }
            ?.let { (instance, _) -> EditInstancesViewModel.ParentInstanceData(instance.instanceKey, instance.name) }

    val dateTime = instances.map { it.instanceDateTime }.minOrNull()!!

    val customTimeDatas = currentCustomTimes.mapValues {
        it.value.let {
            EditInstancesViewModel.CustomTimeData(
                    it.key,
                    it.name,
                    it.hourMinutes.toSortedMap()
            )
        }
    }

    EditInstancesViewModel.Data(instanceKeys.toSet(), parentInstanceData, dateTime, customTimeDatas)
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
        )
    }

    val projects = instances.map { it.task.project }.toSet()

    updateNotifications(now)

    save(dataId, source)

    notifyCloud(projects)

    editInstancesUndoData
}