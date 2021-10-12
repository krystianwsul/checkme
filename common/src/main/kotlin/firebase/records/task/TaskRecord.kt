package com.krystianwsul.common.firebase.records.task

import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.schedule.ScheduleJson
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapperBridge
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.RemoteRecord
import com.krystianwsul.common.firebase.records.noscheduleorparent.NoScheduleOrParentRecord
import com.krystianwsul.common.firebase.records.schedule.*
import com.krystianwsul.common.firebase.records.taskhierarchy.NestedTaskHierarchyRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.*

abstract class TaskRecord protected constructor(
    create: Boolean,
    val id: String,
    private val taskJson: TaskJson,
    val assignedToHelper: AssignedToHelper,
    val projectCustomTimeIdAndKeyProvider: JsonTime.ProjectCustomTimeIdAndKeyProvider,
    override val key: String,
    private val parent: Parent,
    protected val projectHelper: ProjectHelper,
    protected val newProjectRootDelegate: (taskRecord: TaskRecord, scheduleJson: ScheduleJson) -> ProjectRootDelegate,
) : RemoteRecord(create) {

    companion object {

        const val TASKS = "tasks"
    }

    val instanceRecords by lazy {
        taskJson.instances
            .entries
            .associate { (key, instanceJson) ->
                check(key.isNotEmpty())

                val scheduleKey = InstanceRecord.stringToScheduleKey(projectCustomTimeIdAndKeyProvider, key)

                val remoteInstanceRecord = InstanceRecord(
                    create,
                    this,
                    instanceJson,
                    scheduleKey,
                    key,
                )

                scheduleKey to remoteInstanceRecord
            }
            .toMutableMap()
    }

    val singleScheduleRecords: ObservableMap<String, SingleScheduleRecord>
    val weeklyScheduleRecords: ObservableMap<String, WeeklyScheduleRecord>
    val monthlyDayScheduleRecords: ObservableMap<String, MonthlyDayScheduleRecord>
    val monthlyWeekScheduleRecords: ObservableMap<String, MonthlyWeekScheduleRecord>
    val yearlyScheduleRecords: ObservableMap<String, YearlyScheduleRecord>

    abstract val noScheduleOrParentRecords: Map<String, NoScheduleOrParentRecord>

    val taskHierarchyRecords = taskJson.taskHierarchies
        .entries
        .associate { (untypedId, nestedTaskHierarchyJson) ->
            val typedId = TaskHierarchyId(untypedId)

            typedId to NestedTaskHierarchyRecord(typedId, this, nestedTaskHierarchyJson)
        }
        .toMutableMap()

    abstract val name: String
    abstract val note: String?
    abstract val image: TaskJson.Image?

    val startTime get() = taskJson.startTime
    abstract val startTimeOffset: Double?

    abstract val taskKey: TaskKey

    abstract val endData: TaskJson.EndData?

    abstract fun setEndData(endData: RootTaskJson.EndData?)

    var ordinal by Committer(taskJson::ordinal)

    final override val children
        get() = instanceRecords.values +
                singleScheduleRecords.values +
                weeklyScheduleRecords.values +
                monthlyDayScheduleRecords.values +
                monthlyWeekScheduleRecords.values +
                yearlyScheduleRecords.values +
                noScheduleOrParentRecords.values +
                taskHierarchyRecords.values

    init {
        val scheduleWrapperBridges =
            taskJson.schedules.mapValues { it.value to ScheduleWrapperBridge.fromScheduleWrapper(it.value) }

        singleScheduleRecords = scheduleWrapperBridges.filter { it.value.second.singleScheduleJson != null }
            .mapValues { (id, pair) ->
                val (scheduleWrapper, scheduleWrapperBridge) = pair

                @Suppress("LeakingThis")
                SingleScheduleRecord(
                    this,
                    scheduleWrapper,
                    projectHelper,
                    newProjectRootDelegate(this, scheduleWrapperBridge.singleScheduleJson!!),
                    id,
                    false,
                    scheduleWrapperBridge,
                )
            }
            .toObservableMap()

        weeklyScheduleRecords = scheduleWrapperBridges.filter { it.value.second.weeklyScheduleJson != null }
            .mapValues { (id, pair) ->
                val (scheduleWrapper, scheduleWrapperBridge) = pair

                @Suppress("LeakingThis")
                WeeklyScheduleRecord(
                    this,
                    scheduleWrapper,
                    projectHelper,
                    newProjectRootDelegate(this, scheduleWrapperBridge.weeklyScheduleJson!!),
                    id,
                    false,
                    scheduleWrapperBridge,
                )
            }
            .toObservableMap()

        monthlyDayScheduleRecords = scheduleWrapperBridges.filter { it.value.second.monthlyDayScheduleJson != null }
            .mapValues { (id, pair) ->
                val (scheduleWrapper, scheduleWrapperBridge) = pair

                @Suppress("LeakingThis")
                MonthlyDayScheduleRecord(
                    this,
                    scheduleWrapper,
                    projectHelper,
                    newProjectRootDelegate(this, scheduleWrapperBridge.monthlyDayScheduleJson!!),
                    id,
                    false,
                    scheduleWrapperBridge,
                )
            }
            .toObservableMap()

        monthlyWeekScheduleRecords = scheduleWrapperBridges.filter { it.value.second.monthlyWeekScheduleJson != null }
            .mapValues { (id, pair) ->
                val (scheduleWrapper, scheduleWrapperBridge) = pair

                @Suppress("LeakingThis")
                MonthlyWeekScheduleRecord(
                    this,
                    scheduleWrapper,
                    projectHelper,
                    newProjectRootDelegate(this, scheduleWrapperBridge.monthlyWeekScheduleJson!!),
                    id,
                    false,
                    scheduleWrapperBridge,
                )
            }
            .toObservableMap()

        yearlyScheduleRecords = scheduleWrapperBridges.filter { it.value.second.yearlyScheduleJson != null }
            .mapValues { (id, pair) ->
                val (scheduleWrapper, scheduleWrapperBridge) = pair

                @Suppress("LeakingThis")
                YearlyScheduleRecord(
                    this,
                    scheduleWrapper,
                    projectHelper,
                    newProjectRootDelegate(this, scheduleWrapperBridge.yearlyScheduleJson!!),
                    id,
                    false,
                    scheduleWrapperBridge,
                )
            }
            .toObservableMap()
    }

    private val scheduleCustomTimeKeysProperty = invalidatableLazy {
        listOf(
            singleScheduleRecords,
            weeklyScheduleRecords,
            monthlyDayScheduleRecords,
            monthlyWeekScheduleRecords,
            yearlyScheduleRecords,
        ).flatMap { it.values }.map { it.customTimeKey }
    }.apply {
        singleScheduleRecords.callback = ::invalidate
        weeklyScheduleRecords.callback = ::invalidate
        monthlyDayScheduleRecords.callback = ::invalidate
        monthlyWeekScheduleRecords.callback = ::invalidate
        yearlyScheduleRecords.callback = ::invalidate
    }

    val scheduleCustomTimeKeys by scheduleCustomTimeKeysProperty

    fun newInstanceRecord(instanceJson: InstanceJson, instanceScheduleKey: InstanceScheduleKey): InstanceRecord {
        val firebaseKey = InstanceRecord.scheduleKeyToString(instanceScheduleKey)

        val projectInstanceRecord = InstanceRecord(
            true,
            this,
            instanceJson,
            instanceScheduleKey,
            firebaseKey,
        )

        check(!instanceRecords.containsKey(projectInstanceRecord.instanceScheduleKey))

        instanceRecords[projectInstanceRecord.instanceScheduleKey] = projectInstanceRecord
        return projectInstanceRecord
    }

    final override fun deleteFromParent() = parent.deleteTaskRecord(this)

    protected fun getCustomTimeKeys(): List<CustomTimeKey> {
        val instanceCustomTimeKeys = instanceRecords.values.flatMap {
            listOf(it.instanceScheduleKey.scheduleTimePair.customTimeKey, it.instanceCustomTimeKey)
        }

        return listOf(scheduleCustomTimeKeys, instanceCustomTimeKeys).flatten().filterNotNull()
    }

    abstract fun getUserCustomTimeKeys(): Set<CustomTimeKey.User>

    interface Parent {

        fun deleteTaskRecord(taskRecord: TaskRecord)
    }
}
