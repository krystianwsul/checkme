package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.getDomainResultInterrupting
import com.krystianwsul.checkme.domainmodel.undo.UndoData
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.domainmodel.update.SingleDomainUpdate
import com.krystianwsul.checkme.gui.edit.dialogs.parentpicker.ParentInstanceViewModel
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.DomainQuery
import com.krystianwsul.checkme.viewmodels.EditInstancesViewModel
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.MyCustomTime
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.search.SearchContext
import com.krystianwsul.common.locker.LockerManager
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.InstanceKey
import io.reactivex.rxjava3.core.Single

fun DomainFactory.getEditInstancesData(instanceKeys: Set<InstanceKey>): EditInstancesViewModel.Data {
    MyCrashlytics.log("DomainFactory.getEditInstancesData")

    DomainThreadChecker.instance.requireDomainThread()

    check(instanceKeys.isNotEmpty())

    val customTimes = getCurrentRemoteCustomTimes().associate { it.key to it as Time.Custom }.toMutableMap()

    val instances = instanceKeys.map(::getInstance)

    instances.forEach { instance ->
        (instance.instanceTime as? Time.Custom)?.let { customTimes[it.key] = it }
    }

    fun Instance.hierarchyContainsKeys(): Boolean {
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

    val customTimeDatas = customTimes.mapValues {
        it.value.let {
            EditInstancesViewModel.CustomTimeData(
                it.key,
                it.name,
                it.hourMinutes.toSortedMap(),
                it is MyCustomTime,
            )
        }
    }

    return EditInstancesViewModel.Data(parentInstanceData, dateTime, customTimeDatas)
}

private class SetInstancesDateTimeUndoData(val data: List<Pair<InstanceKey, DateTimePair?>>) : UndoData {

    override fun undo(domainFactory: DomainFactory, now: ExactTimeStamp.Local) = domainFactory.run {
        val pairs = data.map { getInstance(it.first) to it.second?.let(::getDateTime) }

        pairs.forEach { (instance, dateTime) ->
            instance.setInstanceDateTime(shownFactory, dateTime, this, now)
        }

        pairs.map { it.first.task.project }.toSet()
    }
}

@CheckResult
fun DomainUpdater.setInstancesDateTime(
    notificationType: DomainListenerManager.NotificationType,
    instanceKeys: Set<InstanceKey>,
    instanceDate: Date,
    instanceTimePair: TimePair,
): Single<EditInstancesResult> = SingleDomainUpdate.create("setInstancesDateTime") { now ->
    check(instanceKeys.isNotEmpty())

    val instances = instanceKeys.map(this::getInstance)

    val editInstancesUndoData = SetInstancesDateTimeUndoData(
        instances.map { it.instanceKey to it.recordInstanceDateTime?.toDateTimePair() }
    )

    val time = getTime(instanceTimePair)

    trackRootTaskIds {
        instances.forEach {
            it.setInstanceDateTime(shownFactory, DateTime(instanceDate, time), this, now)

            if (it.parentInstance != null) {
                when (it.parentState) {
                    is Instance.ParentState.Unset -> it.setParentState(Instance.ParentState.NoParent)
                    is Instance.ParentState.NoParent -> throw IllegalStateException()
                    is Instance.ParentState.Parent -> {
                        val newParentState = if (it.getTaskHierarchyParentInstance() != null)
                            Instance.ParentState.NoParent
                        else
                            Instance.ParentState.Unset

                        it.setParentState(newParentState)
                    }
                }

                check(it.parentInstance == null)
            }
        }
    }

    val projects = instances.map { it.task.project }.toSet()

    val timeStamp = DateTime(instanceDate, time).timeStamp

    DomainUpdater.Result(
        EditInstancesResult(editInstancesUndoData, timeStamp),
        true,
        notificationType,
        DomainFactory.CloudParams(projects),
    )
}.perform(this)

data class EditInstancesResult(val undoData: UndoData, val newTimeStamp: TimeStamp?)

private class ListUndoData(private val undoDatas: List<UndoData>) : UndoData {

    override fun undo(domainFactory: DomainFactory, now: ExactTimeStamp.Local) =
        undoDatas.flatMap { it.undo(domainFactory, now) }.toSet()
}

@CheckResult
fun DomainUpdater.setInstancesParent(
    notificationType: DomainListenerManager.NotificationType,
    instanceKeys: Set<InstanceKey>,
    parentInstanceKey: InstanceKey,
): Single<EditInstancesResult> = SingleDomainUpdate.create("setInstancesParent") {
    check(instanceKeys.isNotEmpty())

    val instances = instanceKeys.map(this::getInstance)

    val originalProjects = instances.map { it.task.project }

    val undoDatas = trackRootTaskIds {
        instances.map {
            val undoData = SetInstanceParentUndoData(it.instanceKey, it.parentState)

            it.setParentState(parentInstanceKey)

            undoData
        }
    }

    val finalProjects = instances.map { it.task.project }.toSet()

    DomainUpdater.Result(
        EditInstancesResult(ListUndoData(undoDatas), null),
        true,
        notificationType,
        DomainFactory.CloudParams(originalProjects + finalProjects),
    )
}.perform(this)

fun DomainFactory.getEditInstancesSearchData(
    searchCriteria: SearchCriteria,
    page: Int,
): DomainQuery<ParentInstanceViewModel.Data> {
    MyCrashlytics.log("DomainFactory.getEditInstancesSearchData")

    DomainThreadChecker.instance.requireDomainThread()

    return LockerManager.setLocker { now ->
        getDomainResultInterrupting {
            val searchContext = SearchContext.startSearch(searchCriteria, now, myUserFactory.user)

            val (instanceEntryDatas, hasMore) = searchInstances<ParentInstanceViewModel.InstanceEntryData>(
                now,
                searchContext,
                page,
                null,
            ) { instance, children, filterResult ->
                val instanceDateTime = instance.instanceDateTime
                val instanceTimeStamp = instanceDateTime.timeStamp

                ParentInstanceViewModel.InstanceEntryData(
                    instance.name,
                    children,
                    instance.instanceKey,
                    if (instance.isRootInstance()) instanceDateTime.getDisplayText() else null,
                    instance.task.note,
                    ParentInstanceViewModel.SortKey(instanceTimeStamp, instance.ordinal),
                    instanceTimeStamp,
                    instance.ordinal,
                    instance.instanceKey,
                    filterResult.matchesSearch,
                    instanceDateTime.toDateTimePair(),
                )
            }

            ParentInstanceViewModel.Data(instanceEntryDatas, hasMore, searchCriteria)
        }
    }
}