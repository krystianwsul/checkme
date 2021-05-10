package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.firebase.json.noscheduleorparent.RootNoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.schedule.*
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.interval.Type
import com.krystianwsul.common.firebase.models.noscheduleorparent.NoScheduleOrParent
import com.krystianwsul.common.firebase.models.noscheduleorparent.RootNoScheduleOrParent
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.schedule.*
import com.krystianwsul.common.firebase.models.taskhierarchy.NestedTaskHierarchy
import com.krystianwsul.common.firebase.models.taskhierarchy.ParentTaskDelegate
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*

class RootTask(
    val taskRecord: RootTaskRecord,
    override val parent: Parent,
    private val userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
) : Task(
    JsonTime.CustomTimeProvider.getForRootTask(userCustomTimeProvider),
    taskRecord,
    ParentTaskDelegate.Root(parent),
) {

    private fun Type.Schedule.getParentProjectSchedule() = taskParentEntries.maxByOrNull { it.startExactTimeStamp }!!

    private val projectIdProperty = invalidatableLazyCallbacks {
        val interval = intervals.last()

        when (val type = interval.type) {
            is Type.Schedule -> type.getParentProjectSchedule().projectId
            is Type.NoSchedule -> (type.noScheduleOrParent as RootNoScheduleOrParent).projectId
            is Type.Child -> {
                val parentTask = type.parentTaskHierarchy.parentTask as RootTask

                parentTask.projectId
            }
        }
    }.apply {
        addTo(intervalsProperty)
        addCallback { normalizedFieldsDelegate.invalidate() }
    }

    val projectId: String by projectIdProperty

    override val project get() = parent.getProject(projectId)

    private val noScheduleOrParentsMap = taskRecord.noScheduleOrParentRecords
        .mapValues { RootNoScheduleOrParent(this, it.value) }
        .toMutableMap()

    override val noScheduleOrParents: Collection<RootNoScheduleOrParent> get() = noScheduleOrParentsMap.values

    override val taskKey get() = TaskKey.Root(taskRecord.id)

    override val projectParentTaskHierarchies = setOf<ProjectTaskHierarchy>()

    override val projectCustomTimeIdProvider = JsonTime.ProjectCustomTimeIdProvider.rootTask

    fun setNoScheduleOrParent(now: ExactTimeStamp.Local, projectKey: ProjectKey<*>) {
        val noScheduleOrParentRecord = taskRecord.newNoScheduleOrParentRecord(
            RootNoScheduleOrParentJson(
                now.long,
                now.offset,
                projectId = projectKey.key,
            )
        )

        check(!noScheduleOrParentsMap.containsKey(noScheduleOrParentRecord.id))

        noScheduleOrParentsMap[noScheduleOrParentRecord.id] =
            RootNoScheduleOrParent(this, noScheduleOrParentRecord)

        invalidateIntervals()
    }

    fun createChildTask(
        now: ExactTimeStamp.Local,
        name: String,
        note: String?,
        image: TaskJson.Image?,
        ordinal: Double?,
    ): RootTask {
        val childTask = parent.createTask(now, image, name, note, ordinal)

        childTask.createParentNestedTaskHierarchy(this, now)
        addRootTask(childTask)

        return childTask
    }

    override fun deleteProjectRootTaskId() = project.removeRootTask(taskKey)
    override fun deleteFromParent() = parent.deleteRootTask(this)

    fun setName(name: String, note: String?) {
        check(name.isNotEmpty())

        taskRecord.name = name
        taskRecord.note = note

        normalizedFieldsDelegate.invalidate()
    }

    override fun getDateTime(scheduleKey: ScheduleKey) =
        DateTime(scheduleKey.scheduleDate, getTime(scheduleKey.scheduleTimePair))

    private fun getTime(timePair: TimePair) = timePair.customTimeKey
        ?.let { userCustomTimeProvider.getUserCustomTime(it as CustomTimeKey.User) }
        ?: Time.Normal(timePair.hourMinute!!)

    override fun getOrCopyTime(
        dayOfWeek: DayOfWeek,
        time: Time,
        customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
        now: ExactTimeStamp.Local,
    ) = when (time) {
        is Time.Custom.Project<*> -> customTimeMigrationHelper.tryMigrateProjectCustomTime(time, now)
            ?: Time.Normal(time.getHourMinute(dayOfWeek))
        is Time.Custom.User -> time
        is Time.Normal -> time
    }

    fun addChild(childTask: RootTask, now: ExactTimeStamp.Local): TaskHierarchyKey {
        val taskHierarchyKey = childTask.createParentNestedTaskHierarchy(this, now)
        addRootTask(childTask)

        return taskHierarchyKey
    }

    private fun createParentNestedTaskHierarchy(parentTask: Task, now: ExactTimeStamp.Local): TaskHierarchyKey.Nested {
        val taskHierarchyJson = NestedTaskHierarchyJson(parentTask.id, now.long, now.offset)

        return createParentNestedTaskHierarchy(taskHierarchyJson).taskHierarchyKey
    }

    private fun createParentNestedTaskHierarchy(nestedTaskHierarchyJson: NestedTaskHierarchyJson): NestedTaskHierarchy {
        val taskHierarchyRecord = taskRecord.newTaskHierarchyRecord(nestedTaskHierarchyJson)
        val taskHierarchy = NestedTaskHierarchy(this, taskHierarchyRecord, parentTaskDelegate)

        nestedParentTaskHierarchies[taskHierarchy.id] = taskHierarchy

        taskHierarchy.invalidateTasks()

        return taskHierarchy
    }

    fun copyParentNestedTaskHierarchy(
        now: ExactTimeStamp.Local,
        startTaskHierarchy: TaskHierarchy,
        parentTaskId: String,
    ) {
        check(parentTaskId.isNotEmpty())

        val taskHierarchyJson = NestedTaskHierarchyJson(
            parentTaskId,
            now.long,
            now.offset,
            startTaskHierarchy.endExactTimeStampOffset?.long,
            startTaskHierarchy.endExactTimeStampOffset?.offset,
        )

        createParentNestedTaskHierarchy(taskHierarchyJson)
    }

    fun deleteNoScheduleOrParent(noScheduleOrParent: NoScheduleOrParent) {
        check(noScheduleOrParentsMap.containsKey(noScheduleOrParent.id))

        noScheduleOrParentsMap.remove(noScheduleOrParent.id)
        invalidateIntervals()
    }

    fun addRootTask(childTask: RootTask) {
        taskRecord.rootTaskParentDelegate.addRootTask(childTask.taskKey) { parent.updateTaskRecord(taskKey, it) }
    }

    fun removeRootTask(childTask: RootTask) {
        taskRecord.rootTaskParentDelegate.removeRootTask(childTask.taskKey) { parent.updateTaskRecord(taskKey, it) }
    }

    override fun invalidateProjectParentTaskHierarchies() = invalidateIntervals()

    fun updateProject(projectKey: ProjectKey<*>): RootTask {
        if (project.projectKey == projectKey) return this

        val interval = intervals.last()

        when (val type = interval.type) {
            is Type.Schedule -> type.getParentProjectSchedule()
            is Type.NoSchedule -> type.noScheduleOrParent!!
            is Type.Child -> null // called redundantly
        }?.updateProject(projectKey)

        return this
    }

    private data class ScheduleDiffKey(val scheduleData: ScheduleData, val assignedTo: Set<UserKey>)

    fun updateSchedules(
        shownFactory: Instance.ShownFactory,
        scheduleDatas: List<Pair<ScheduleData, Time>>,
        now: ExactTimeStamp.Local,
        assignedTo: Set<UserKey>,
        customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
        projectKey: ProjectKey<*>,
    ) {
        val removeSchedules = mutableListOf<Schedule>()
        val addScheduleDatas = scheduleDatas.map { ScheduleDiffKey(it.first, assignedTo) to it }.toMutableList()

        val oldSchedules = getCurrentScheduleIntervals(now).map { it.schedule }

        val oldScheduleDatas = ScheduleGroup.getGroups(oldSchedules).map {
            ScheduleDiffKey(it.scheduleData, it.assignedTo) to it.schedules
        }

        for ((key, value) in oldScheduleDatas) {
            val existing = addScheduleDatas.singleOrNull { it.first == key }

            if (existing != null)
                addScheduleDatas.remove(existing)
            else
                removeSchedules.addAll(value)
        }

        /*
            requirements for mock:
                there was one old schedule, it was single and mocked, and it's getting replaced
                by another single schedule
         */

        val singleRemoveSchedule = removeSchedules.singleOrNull() as? SingleSchedule

        val singleAddSchedulePair = addScheduleDatas.singleOrNull()?.takeIf {
            it.first.scheduleData is ScheduleData.Single
        }

        if (singleRemoveSchedule != null && singleAddSchedulePair != null) {
            if (assignedTo.isNotEmpty()) singleRemoveSchedule.setAssignedTo(assignedTo)

            singleRemoveSchedule.getInstance(this).setInstanceDateTime(
                shownFactory,
                singleAddSchedulePair.second.run { DateTime((first as ScheduleData.Single).date, second) },
                customTimeMigrationHelper,
                now,
            )
        } else {
            removeSchedules.forEach { it.setEndExactTimeStamp(now.toOffset()) }

            createSchedules(
                now,
                addScheduleDatas.map { it.second },
                assignedTo,
                customTimeMigrationHelper,
                projectKey,
            )
        }
    }

    fun createSchedules(
        now: ExactTimeStamp.Local,
        scheduleDatas: List<Pair<ScheduleData, Time>>,
        assignedTo: Set<UserKey>,
        customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
        projectKey: ProjectKey<*>,
    ) {
        val assignedToKeys = assignedTo.map { it.key }
            .toSet()
            .toAssociateMap()

        for ((scheduleData, time) in scheduleDatas) {
            when (scheduleData) {
                is ScheduleData.Single -> {
                    val date = scheduleData.date

                    val copiedTime = getOrCopyTime(
                        date.dayOfWeek,
                        time,
                        customTimeMigrationHelper,
                        now,
                    )

                    val singleScheduleRecord = taskRecord.newSingleScheduleRecord(
                        RootSingleScheduleJson(
                            now.long,
                            now.offset,
                            null,
                            null,
                            date.year,
                            date.month,
                            date.day,
                            assignedToKeys,
                            copiedTime.toJson(),
                            projectKey.key,
                        )
                    )

                    _schedules += SingleSchedule(this, singleScheduleRecord)
                }
                is ScheduleData.Weekly -> {
                    for (dayOfWeek in scheduleData.daysOfWeek) {
                        val copiedTime = getOrCopyTime(
                            dayOfWeek,
                            time,
                            customTimeMigrationHelper,
                            now,
                        )

                        val weeklyScheduleRecord = taskRecord.newWeeklyScheduleRecord(
                            RootWeeklyScheduleJson(
                                now.long,
                                now.offset,
                                null,
                                null,
                                dayOfWeek.ordinal,
                                scheduleData.from?.toJson(),
                                scheduleData.until?.toJson(),
                                scheduleData.interval,
                                assignedTo = assignedToKeys,
                                time = copiedTime.toJson(),
                                projectId = projectKey.key,
                            )
                        )

                        _schedules += WeeklySchedule(this, weeklyScheduleRecord)
                    }
                }
                is ScheduleData.MonthlyDay -> {
                    val (dayOfMonth, beginningOfMonth, _) = scheduleData

                    val today = Date.today()

                    val dayOfWeek = getDateInMonth(
                        today.year,
                        today.month,
                        scheduleData.dayOfMonth,
                        scheduleData.beginningOfMonth,
                    ).dayOfWeek

                    val copiedTime = getOrCopyTime(dayOfWeek, time, customTimeMigrationHelper, now)

                    val monthlyDayScheduleRecord = taskRecord.newMonthlyDayScheduleRecord(
                        RootMonthlyDayScheduleJson(
                            now.long,
                            now.offset,
                            null,
                            null,
                            dayOfMonth,
                            beginningOfMonth,
                            scheduleData.from?.toJson(),
                            scheduleData.until?.toJson(),
                            assignedTo = assignedToKeys,
                            time = copiedTime.toJson(),
                            projectId = projectKey.key,
                        )
                    )

                    _schedules += MonthlyDaySchedule(this, monthlyDayScheduleRecord)
                }
                is ScheduleData.MonthlyWeek -> {
                    val (weekOfMonth, dayOfWeek, beginningOfMonth) = scheduleData
                    val copiedTime = getOrCopyTime(dayOfWeek, time, customTimeMigrationHelper, now)

                    val monthlyWeekScheduleRecord = taskRecord.newMonthlyWeekScheduleRecord(
                        RootMonthlyWeekScheduleJson(
                            now.long,
                            now.offset,
                            null,
                            null,
                            weekOfMonth,
                            dayOfWeek.ordinal,
                            beginningOfMonth,
                            scheduleData.from?.toJson(),
                            scheduleData.until?.toJson(),
                            assignedTo = assignedToKeys,
                            time = copiedTime.toJson(),
                            projectId = projectKey.key,
                        )
                    )

                    _schedules += MonthlyWeekSchedule(this, monthlyWeekScheduleRecord)
                }
                is ScheduleData.Yearly -> {
                    val copiedTime = getOrCopyTime(
                        Date(Date.today().year, scheduleData.month, scheduleData.day).dayOfWeek,
                        time,
                        customTimeMigrationHelper,
                        now,
                    )

                    val yearlyScheduleRecord = taskRecord.newYearlyScheduleRecord(
                        RootYearlyScheduleJson(
                            now.long,
                            now.offset,
                            null,
                            null,
                            scheduleData.month,
                            scheduleData.day,
                            scheduleData.from?.toJson(),
                            scheduleData.until?.toJson(),
                            assignedTo = assignedToKeys,
                            time = copiedTime.toJson(),
                            projectId = projectKey.key,
                        )
                    )

                    _schedules += YearlySchedule(this, yearlyScheduleRecord)
                }
            }
        }

        intervalsProperty.invalidate()
    }

    private fun Set<String>.toAssociateMap() = associate { it to true }

    private fun Time.toJson() = JsonTime.fromTime(this).toJson()

    fun copySchedules(
        now: ExactTimeStamp.Local,
        schedules: List<Schedule>,
        customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
        oldProjectKey: ProjectKey<*>,
        newProjectKey: ProjectKey<*>,
    ) {
        for (schedule in schedules) {
            val today = Date.today()

            val dayOfWeek = when (schedule) {
                is SingleSchedule -> schedule.date.dayOfWeek
                is WeeklySchedule -> schedule.dayOfWeek
                is MonthlyDaySchedule -> schedule.getDateInMonth(today.year, today.month).dayOfWeek
                is MonthlyWeekSchedule -> schedule.dayOfWeek
                is YearlySchedule -> schedule.getDateInYear(today.year).dayOfWeek
                else -> throw UnsupportedOperationException()
            }

            val assignedTo = schedule.takeIf { oldProjectKey == newProjectKey }
                ?.assignedTo
                .orEmpty()
                .map { it.key }
                .toSet()
                .toAssociateMap()

            val timeJson = getOrCopyTime(
                dayOfWeek,
                schedule.time,
                customTimeMigrationHelper,
                now,
            ).toJson()

            when (schedule) {
                is SingleSchedule -> {
                    val date = schedule.date

                    val singleScheduleRecord = taskRecord.newSingleScheduleRecord(
                        RootSingleScheduleJson(
                            now.long,
                            now.offset,
                            schedule.endExactTimeStamp?.long,
                            schedule.endExactTimeStamp?.offset,
                            date.year,
                            date.month,
                            date.day,
                            assignedTo,
                            timeJson,
                            newProjectKey.key,
                        )
                    )

                    _schedules += SingleSchedule(this, singleScheduleRecord)
                }
                is WeeklySchedule -> {
                    val weeklyScheduleRecord = taskRecord.newWeeklyScheduleRecord(
                        RootWeeklyScheduleJson(
                            now.long,
                            now.offset,
                            schedule.endExactTimeStamp?.long,
                            schedule.endExactTimeStamp?.offset,
                            schedule.dayOfWeek.ordinal,
                            schedule.from?.toJson(),
                            schedule.until?.toJson(),
                            schedule.interval,
                            assignedTo = assignedTo,
                            time = timeJson,
                            projectId = newProjectKey.key,
                        )
                    )

                    _schedules += WeeklySchedule(this, weeklyScheduleRecord)
                }
                is MonthlyDaySchedule -> {
                    val monthlyDayScheduleRecord = taskRecord.newMonthlyDayScheduleRecord(
                        RootMonthlyDayScheduleJson(
                            now.long,
                            now.offset,
                            schedule.endExactTimeStamp?.long,
                            schedule.endExactTimeStamp?.offset,
                            schedule.dayOfMonth,
                            schedule.beginningOfMonth,
                            schedule.from?.toJson(),
                            schedule.until?.toJson(),
                            assignedTo = assignedTo,
                            time = timeJson,
                            projectId = newProjectKey.key,
                        )
                    )

                    _schedules += MonthlyDaySchedule(this, monthlyDayScheduleRecord)
                }
                is MonthlyWeekSchedule -> {
                    val monthlyWeekScheduleRecord = taskRecord.newMonthlyWeekScheduleRecord(
                        RootMonthlyWeekScheduleJson(
                            now.long,
                            now.offset,
                            schedule.endExactTimeStamp?.long,
                            schedule.endExactTimeStamp?.offset,
                            schedule.weekOfMonth,
                            schedule.dayOfWeek.ordinal,
                            schedule.beginningOfMonth,
                            schedule.from?.toJson(),
                            schedule.until?.toJson(),
                            assignedTo = assignedTo,
                            time = timeJson,
                            projectId = newProjectKey.key,
                        )
                    )

                    _schedules += MonthlyWeekSchedule(this, monthlyWeekScheduleRecord)
                }
                is YearlySchedule -> {
                    val yearlyScheduleRecord = taskRecord.newYearlyScheduleRecord(
                        RootYearlyScheduleJson(
                            now.long,
                            now.offset,
                            schedule.endExactTimeStamp?.long,
                            schedule.endExactTimeStamp?.offset,
                            schedule.month,
                            schedule.day,
                            schedule.from?.toJson(),
                            schedule.until?.toJson(),
                            assignedTo = assignedTo,
                            time = timeJson,
                            projectId = newProjectKey.key,
                        )
                    )

                    _schedules += YearlySchedule(this, yearlyScheduleRecord)
                }
            }
        }

        intervalsProperty.invalidate()
    }

    interface Parent : Task.Parent, Project.RootTaskProvider {

        fun deleteRootTask(task: RootTask)

        fun getProject(projectId: String): Project<*>

        override fun createTask(
            now: ExactTimeStamp.Local,
            image: TaskJson.Image?,
            name: String,
            note: String?,
            ordinal: Double?,
        ): RootTask
    }
}
