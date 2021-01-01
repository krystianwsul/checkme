package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactory.Companion.syncOnDomain
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.EditInstancesUndoData
import com.krystianwsul.checkme.domainmodel.getDomainResultInterrupting
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.DomainResult
import com.krystianwsul.checkme.viewmodels.EditInstancesSearchViewModel
import com.krystianwsul.checkme.viewmodels.EditInstancesViewModel
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.models.Instance
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

    fun Instance<*>.hierarchyContainsKeys(): Boolean {
        if (instanceKey in instanceKeys) return true

        return parentInstanceData?.instance
                ?.hierarchyContainsKeys()
                ?: false
    }

    val parentInstanceData = instances.mapNotNull {
        it.parentInstanceData
                ?.instance
                ?.takeUnless { it.hierarchyContainsKeys() }
    }
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
        source: SaveService.Source,
        instanceKeys: Set<InstanceKey>,
        instanceDate: Date,
        instanceTimePair: TimePair,
): EditInstancesUndoData = syncOnDomain {
    MyCrashlytics.log("DomainFactory.setInstancesDateTime")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    editInstances(source, instanceKeys) { instances ->
        instances.forEach {
            it.setInstanceDateTime(
                    localFactory,
                    ownerKey,
                    DateTime(instanceDate, getTime(instanceTimePair)),
            )
        }
    }
}

fun DomainFactory.setInstancesParent(
        source: SaveService.Source,
        instanceKeys: Set<InstanceKey>,
        parentInstanceKey: InstanceKey,
): EditInstancesUndoData = syncOnDomain {
    MyCrashlytics.log("DomainFactory.setInstancesParent")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    editInstances(source, instanceKeys) { instances ->
        instances.forEach { it.setParentState(Instance.ParentState.Parent(parentInstanceKey)) }
    }
}

private fun DomainFactory.editInstances(
        source: SaveService.Source,
        instanceKeys: Set<InstanceKey>,
        applyChange: (List<Instance<*>>) -> Unit,
): EditInstancesUndoData = syncOnDomain {
    check(instanceKeys.isNotEmpty())

    val now = ExactTimeStamp.Local.now

    val instances = instanceKeys.map(this::getInstance)

    val editInstancesUndoData = EditInstancesUndoData(
            instances.map {
                Pair(
                        it.instanceKey,
                        EditInstancesUndoData.Anchor(it.parentState, it.recordInstanceDateTime?.toDateTimePair())
                )
            }
    )

    applyChange(instances)

    val projects = instances.map { it.task.project }.toSet()

    updateNotifications(now)

    save(DomainListenerManager.NotificationType.All, source)

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
                        instance.run { EditInstancesSearchViewModel.SortKey(instanceDateTime.timeStamp, task.ordinal) },
                        instance.instanceDateTime.timeStamp,
                        instance.task.ordinal,
                        instance.instanceKey,
                )
            }

            EditInstancesSearchViewModel.Data(instanceEntryDatas, hasMore, searchCriteria)
        }
    }
}