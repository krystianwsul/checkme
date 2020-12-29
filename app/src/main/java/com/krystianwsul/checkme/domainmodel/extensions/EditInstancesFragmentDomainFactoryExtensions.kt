package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactory.Companion.syncOnDomain
import com.krystianwsul.checkme.domainmodel.getDomainResultInterrupting
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.DomainResult
import com.krystianwsul.checkme.viewmodels.EditInstancesSearchViewModel
import com.krystianwsul.checkme.viewmodels.EditInstancesViewModel
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.locker.LockerManager
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

fun DomainFactory.getEditInstancesSearchData(
        searchCriteria: SearchCriteria,
        page: Int,
): DomainResult<EditInstancesSearchViewModel.Data> = syncOnDomain {
    MyCrashlytics.log("DomainFactory.getEditInstancesSearchData")

    LockerManager.setLocker { now ->
        getDomainResultInterrupting {
            val (instanceEntryDatas, hasMore) = searchInstances<EditInstancesSearchViewModel.InstanceEntryData>(
                    now,
                    searchCriteria,
                    page,
            ) { instance, _, children ->
                EditInstancesSearchViewModel.InstanceEntryData(
                        instance.name,
                        children.values,
                        instance.instanceKey,
                        if (instance.isRootInstance()) instance.instanceDateTime.getDisplayText() else null,
                        instance.task.note,
                        EditInstancesSearchViewModel.SortKey(instance.task.startExactTimeStamp),
                        instance.instanceDateTime.timeStamp,
                        instance.task.ordinal,
                )
            }

            EditInstancesSearchViewModel.Data(instanceEntryDatas, hasMore, searchCriteria)
        }
    }
}