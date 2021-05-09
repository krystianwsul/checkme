package com.krystianwsul.common.firebase.records.task

import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.noscheduleorparent.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.schedule.*
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.RemoteRecord
import com.krystianwsul.common.firebase.records.noscheduleorparent.NoScheduleOrParentRecord
import com.krystianwsul.common.firebase.records.schedule.*
import com.krystianwsul.common.firebase.records.taskhierarchy.NestedTaskHierarchyRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ScheduleKey
import com.krystianwsul.common.utils.TaskHierarchyId
import com.krystianwsul.common.utils.TaskKey

abstract class TaskRecord protected constructor(
    create: Boolean,
    val id: String,
    private val taskJson: TaskJson,
    val assignedToHelper: AssignedToHelper,
    val projectCustomTimeIdAndKeyProvider: JsonTime.ProjectCustomTimeIdAndKeyProvider,
    override val key: String,
    private val parent: Parent,
    private val projectHelper: ProjectHelper,
    private val newProjectRootDelegate: (taskRecord: TaskRecord, scheduleJson: ScheduleJson) -> ProjectRootDelegate,
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

    val singleScheduleRecords: MutableMap<String, SingleScheduleRecord> = mutableMapOf()

    val weeklyScheduleRecords: MutableMap<String, WeeklyScheduleRecord> = mutableMapOf()

    val monthlyDayScheduleRecords: MutableMap<String, MonthlyDayScheduleRecord> = mutableMapOf()

    val monthlyWeekScheduleRecords: MutableMap<String, MonthlyWeekScheduleRecord> = mutableMapOf()

    val yearlyScheduleRecords: MutableMap<String, YearlyScheduleRecord> = mutableMapOf()

    abstract val noScheduleOrParentRecords: Map<String, NoScheduleOrParentRecord>

    val taskHierarchyRecords = taskJson.taskHierarchies
        .entries
        .associate { (untypedId, nestedTaskHierarchyJson) ->
            val typedId = TaskHierarchyId(untypedId)

            typedId to NestedTaskHierarchyRecord(typedId, this, nestedTaskHierarchyJson)
        }
        .toMutableMap()

    var name by Committer(taskJson::name)

    val startTime get() = taskJson.startTime
    abstract val startTimeOffset: Double?

    abstract val taskKey: TaskKey

    abstract var endData: TaskJson.EndData?

    var note by Committer(taskJson::note)

    var image by Committer(taskJson::image)

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
        for ((id, scheduleWrapper) in taskJson.schedules) {
            check(id.isNotEmpty())

            val scheduleWrapperBridge = ScheduleWrapperBridge.fromScheduleWrapper(scheduleWrapper)

            when {
                scheduleWrapperBridge.singleScheduleJson != null -> {
                    check(scheduleWrapperBridge.weeklyScheduleJson == null)
                    check(scheduleWrapperBridge.monthlyDayScheduleJson == null)
                    check(scheduleWrapperBridge.monthlyWeekScheduleJson == null)
                    check(scheduleWrapperBridge.yearlyScheduleJson == null)

                    @Suppress("LeakingThis")
                    singleScheduleRecords[id] = SingleScheduleRecord(
                        this,
                        scheduleWrapper,
                        projectHelper,
                        newProjectRootDelegate(this, scheduleWrapperBridge.singleScheduleJson!!),
                        id,
                        scheduleWrapperBridge,
                    )
                }
                scheduleWrapperBridge.weeklyScheduleJson != null -> {
                    check(scheduleWrapperBridge.monthlyDayScheduleJson == null)
                    check(scheduleWrapperBridge.monthlyWeekScheduleJson == null)
                    check(scheduleWrapperBridge.yearlyScheduleJson == null)

                    @Suppress("LeakingThis")
                    weeklyScheduleRecords[id] = WeeklyScheduleRecord(
                        this,
                        scheduleWrapper,
                        projectHelper,
                        newProjectRootDelegate(this, scheduleWrapperBridge.weeklyScheduleJson!!),
                        id,
                        scheduleWrapperBridge,
                    )
                }
                scheduleWrapperBridge.monthlyDayScheduleJson != null -> {
                    check(scheduleWrapperBridge.monthlyWeekScheduleJson == null)
                    check(scheduleWrapperBridge.yearlyScheduleJson == null)

                    @Suppress("LeakingThis")
                    monthlyDayScheduleRecords[id] = MonthlyDayScheduleRecord(
                        this,
                        scheduleWrapper,
                        projectHelper,
                        newProjectRootDelegate(this, scheduleWrapperBridge.monthlyDayScheduleJson!!),
                        id,
                        scheduleWrapperBridge,
                    )
                }
                scheduleWrapperBridge.monthlyWeekScheduleJson != null -> {
                    check(scheduleWrapperBridge.yearlyScheduleJson == null)

                    @Suppress("LeakingThis")
                    monthlyWeekScheduleRecords[id] = MonthlyWeekScheduleRecord(
                        this,
                        scheduleWrapper,
                        projectHelper,
                        newProjectRootDelegate(this, scheduleWrapperBridge.monthlyWeekScheduleJson!!),
                        id,
                        scheduleWrapperBridge,
                    )
                }
                else -> {
                    check(scheduleWrapperBridge.yearlyScheduleJson != null)

                    @Suppress("LeakingThis")
                    yearlyScheduleRecords[id] = YearlyScheduleRecord(
                        this,
                        scheduleWrapper,
                        projectHelper,
                        newProjectRootDelegate(this, scheduleWrapperBridge.yearlyScheduleJson!!),
                        id,
                        scheduleWrapperBridge,
                    )
                }
            }
        }
    }

    fun newInstanceRecord(instanceJson: InstanceJson, scheduleKey: ScheduleKey): InstanceRecord {
        val firebaseKey = InstanceRecord.scheduleKeyToString(scheduleKey)

        val projectInstanceRecord = InstanceRecord(
            true,
            this,
            instanceJson,
            scheduleKey,
            firebaseKey,
        )

        check(!instanceRecords.containsKey(projectInstanceRecord.scheduleKey))

        instanceRecords[projectInstanceRecord.scheduleKey] = projectInstanceRecord
        return projectInstanceRecord
    }

    protected abstract fun newScheduleWrapper(
        singleScheduleJson: SingleScheduleJson? = null,
        weeklyScheduleJson: WeeklyScheduleJson? = null,
        monthlyDayScheduleJson: MonthlyDayScheduleJson? = null,
        monthlyWeekScheduleJson: MonthlyWeekScheduleJson? = null,
        yearlyScheduleJson: YearlyScheduleJson? = null,
    ): ScheduleWrapper

    fun newSingleScheduleRecord(singleScheduleJson: SingleScheduleJson): SingleScheduleRecord {
        val singleScheduleRecord = SingleScheduleRecord(
            this,
            newScheduleWrapper(singleScheduleJson = singleScheduleJson),
            projectHelper,
            newProjectRootDelegate(this, singleScheduleJson),
        )

        check(!singleScheduleRecords.containsKey(singleScheduleRecord.id))

        singleScheduleRecords[singleScheduleRecord.id] = singleScheduleRecord
        return singleScheduleRecord
    }

    fun newWeeklyScheduleRecord(weeklyScheduleJson: WeeklyScheduleJson): WeeklyScheduleRecord {
        val weeklyScheduleRecord = WeeklyScheduleRecord(
            this,
            newScheduleWrapper(weeklyScheduleJson = weeklyScheduleJson),
            projectHelper,
            newProjectRootDelegate(this, weeklyScheduleJson),
        )

        check(!weeklyScheduleRecords.containsKey(weeklyScheduleRecord.id))

        weeklyScheduleRecords[weeklyScheduleRecord.id] = weeklyScheduleRecord
        return weeklyScheduleRecord
    }

    fun newMonthlyDayScheduleRecord(monthlyDayScheduleJson: MonthlyDayScheduleJson): MonthlyDayScheduleRecord {
        val monthlyDayScheduleRecord = MonthlyDayScheduleRecord(
            this,
            newScheduleWrapper(monthlyDayScheduleJson = monthlyDayScheduleJson),
            projectHelper,
            newProjectRootDelegate(this, monthlyDayScheduleJson),
        )

        check(!monthlyDayScheduleRecords.containsKey(monthlyDayScheduleRecord.id))

        monthlyDayScheduleRecords[monthlyDayScheduleRecord.id] = monthlyDayScheduleRecord
        return monthlyDayScheduleRecord
    }

    fun newMonthlyWeekScheduleRecord(monthlyWeekScheduleJson: MonthlyWeekScheduleJson): MonthlyWeekScheduleRecord {
        val monthlyWeekScheduleRecord = MonthlyWeekScheduleRecord(
            this,
            newScheduleWrapper(monthlyWeekScheduleJson = monthlyWeekScheduleJson),
            projectHelper,
            newProjectRootDelegate(this, monthlyWeekScheduleJson),
        )

        check(!monthlyWeekScheduleRecords.containsKey(monthlyWeekScheduleRecord.id))

        monthlyWeekScheduleRecords[monthlyWeekScheduleRecord.id] = monthlyWeekScheduleRecord
        return monthlyWeekScheduleRecord
    }

    fun newYearlyScheduleRecord(yearlyScheduleJson: YearlyScheduleJson): YearlyScheduleRecord {
        val yearlyScheduleRecord = YearlyScheduleRecord(
            this,
            newScheduleWrapper(yearlyScheduleJson = yearlyScheduleJson),
            projectHelper,
            newProjectRootDelegate(this, yearlyScheduleJson),
        )

        check(!yearlyScheduleRecords.containsKey(yearlyScheduleRecord.id))

        yearlyScheduleRecords[yearlyScheduleRecord.id] = yearlyScheduleRecord
        return yearlyScheduleRecord
    }

    abstract fun newNoScheduleOrParentRecord(noScheduleOrParentJson: NoScheduleOrParentJson): NoScheduleOrParentRecord

    fun newTaskHierarchyRecord(taskHierarchyJson: NestedTaskHierarchyJson): NestedTaskHierarchyRecord {
        val taskHierarchyRecord = NestedTaskHierarchyRecord(this, taskHierarchyJson)
        check(!taskHierarchyRecords.containsKey(taskHierarchyRecord.id))

        taskHierarchyRecords[taskHierarchyRecord.id] = taskHierarchyRecord
        return taskHierarchyRecord
    }

    abstract fun getScheduleRecordId(): String
    abstract fun newNoScheduleOrParentRecordId(): String
    abstract fun newTaskHierarchyRecordId(): TaskHierarchyId

    final override fun deleteFromParent() = parent.deleteTaskRecord(this)

    interface Parent {

        fun deleteTaskRecord(taskRecord: TaskRecord)
    }
}
