package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactory.Companion.syncOnDomain
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.getDomainResultInterrupting
import com.krystianwsul.checkme.domainmodel.undo.UndoData
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
import com.krystianwsul.common.utils.ProjectKey

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

        return parentInstance?.hierarchyContainsKeys() ?: false
    }

    val parentInstanceData = instances.mapNotNull {
        it.parentInstance?.takeUnless { it.hierarchyContainsKeys() }
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

private class SetInstancesDateTimeUndoData(val data: List<Pair<InstanceKey, DateTimePair?>>) : UndoData {

    override fun undo(domainFactory: DomainFactory, now: ExactTimeStamp.Local) = domainFactory.run {
        val pairs = data.map { getInstance(it.first) to it.second?.let(::getDateTime) }

        pairs.forEach { (instance, dateTime) -> instance.setInstanceDateTime(localFactory, ownerKey, dateTime) }

        pairs.map { it.first.task.project }.toSet()
    }
}

fun DomainFactory.setInstancesDateTime(
        source: SaveService.Source,
        instanceKeys: Set<InstanceKey>,
        instanceDate: Date,
        instanceTimePair: TimePair,
): UndoData = syncOnDomain {
    MyCrashlytics.log("DomainFactory.setInstancesDateTime")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    check(instanceKeys.isNotEmpty())

    val now = ExactTimeStamp.Local.now


    val instances = instanceKeys.map(this::getInstance)

    val editInstancesUndoData = SetInstancesDateTimeUndoData(
            instances.map { it.instanceKey to it.recordInstanceDateTime?.toDateTimePair() }
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

    save(DomainListenerManager.NotificationType.All, source)

    notifyCloud(projects)

    editInstancesUndoData
}

private class ListUndoData(private val undoDatas: List<UndoData>) : UndoData {

    override fun undo(domainFactory: DomainFactory, now: ExactTimeStamp.Local) =
            undoDatas.flatMap { it.undo(domainFactory, now) }.toSet()
}

private class SetInstanceParentUndoData(
        private val instanceKey: InstanceKey,
        private val parentState: Instance.ParentState,
) : UndoData {

    override fun undo(
            domainFactory: DomainFactory,
            now: ExactTimeStamp.Local,
    ) = domainFactory.getInstance(instanceKey).let {
        it.setParentState(parentState)

        setOf(it.task.project)
    }
}

fun DomainFactory.setInstancesParent(
        source: SaveService.Source,
        instanceKeys: Set<InstanceKey>,
        parentInstanceKey: InstanceKey,
): UndoData = syncOnDomain {
    MyCrashlytics.log("DomainFactory.setInstancesParent")
    if (projectsFactory.isSaved) throw SavedFactoryException()

    val now = ExactTimeStamp.Local.now

    check(instanceKeys.isNotEmpty())

    val instances = instanceKeys.map(this::getInstance)

    val parentTask = getTaskForce(parentInstanceKey.taskKey)

    val parentTaskHasOtherInstances = parentTask.hasOtherVisibleInstances(now, parentInstanceKey)

    val undoDatas = instances.map {
        if (parentTaskHasOtherInstances || it.task.hasOtherVisibleInstances(now, it.instanceKey)) {
            val undoData = SetInstanceParentUndoData(it.instanceKey, it.parentState)

            it.setParentState(Instance.ParentState.Parent(parentInstanceKey))

            undoData
        } else {
            addChildToParent(it.task, parentTask, now)
        }
    }

    val projects = instances.map { it.task.project }.toSet()

    updateNotifications(now)

    save(DomainListenerManager.NotificationType.All, source)

    notifyCloud(projects)

    ListUndoData(undoDatas)
}

fun DomainFactory.getEditInstancesSearchData(
        searchCriteria: SearchCriteria,
        page: Int,
        projectKey: ProjectKey<*>?,
): DomainResult<EditInstancesSearchViewModel.Data> = syncOnDomain {
    MyCrashlytics.log("DomainFactory.getEditInstancesSearchData")

    LockerManager.setLocker { now ->
        getDomainResultInterrupting {
            val (instanceEntryDatas, hasMore) = searchInstances<EditInstancesSearchViewModel.InstanceEntryData>(
                    now,
                    searchCriteria,
                    page,
                    projectKey,
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