package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.json.schedule.*
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.firebase.models.ProjectIdOwner
import com.krystianwsul.common.firebase.models.TaskParentEntry
import com.krystianwsul.common.firebase.models.cache.*
import com.krystianwsul.common.firebase.models.interval.Type
import com.krystianwsul.common.firebase.models.noscheduleorparent.NoScheduleOrParent
import com.krystianwsul.common.firebase.models.noscheduleorparent.RootNoScheduleOrParent
import com.krystianwsul.common.firebase.models.project.OwnedProject
import com.krystianwsul.common.firebase.models.schedule.*
import com.krystianwsul.common.firebase.models.taskhierarchy.NestedTaskHierarchy
import com.krystianwsul.common.firebase.models.taskhierarchy.ParentTaskDelegate
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*

class RootTask private constructor(
    val taskRecord: RootTaskRecord,
    override val parent: Parent,
    val userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
    clearableInvalidatableManager: ClearableInvalidatableManager,
) : Task(
    JsonTime.CustomTimeProvider.getForRootTask(userCustomTimeProvider),
    taskRecord,
    ParentTaskDelegate.Factory.Root(parent),
    clearableInvalidatableManager,
    parent.rootModelChangeManager,
) {

    constructor(
        taskRecord: RootTaskRecord,
        parent: Parent,
        userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
    ) : this(taskRecord, parent, userCustomTimeProvider, ClearableInvalidatableManager())

    private fun Type.Schedule.getParentProjectSchedule() = taskParentEntries.sortedWith(
        compareByDescending<Schedule> { it.startExactTimeStamp }.thenByDescending { it.id }
    ).first()

    fun getProjectIdTaskParentEntry(): TaskParentEntry {
        val interval = intervalInfo.intervals.last()

        return when (val type = interval.type) {
            is Type.Schedule -> type.getParentProjectSchedule()
            is Type.NoSchedule -> type.noScheduleOrParent
                ?.let { it as? RootNoScheduleOrParent }
                ?: throw NoScheduleOrParentException()
            is Type.Child -> type.parentTaskHierarchy
        }
    }

    private val projectIdCache: InvalidatableCache<String> =
        invalidatableCache(clearableInvalidatableManager) { invalidatableCache ->
            val intervalInfoRemovable = intervalInfoCache.invalidatableManager.addInvalidatable(invalidatableCache)

            when (val taskParentEntry = getProjectIdTaskParentEntry()) {
                is Schedule ->
                    InvalidatableCache.ValueHolder(taskParentEntry.projectId) { intervalInfoRemovable.remove() }
                is RootNoScheduleOrParent ->
                    InvalidatableCache.ValueHolder(taskParentEntry.projectId) { intervalInfoRemovable.remove() }
                is NestedTaskHierarchy -> {
                    val parentTask = taskParentEntry.parentTask as RootTask
                    val projectId = parentTask.projectId

                    val parentTaskRemovable = parentTask.addProjectIdInvalidatable(invalidatableCache)

                    InvalidatableCache.ValueHolder(projectId) {
                        intervalInfoRemovable.remove()
                        parentTaskRemovable.remove()
                    }
                }
                else -> throw IllegalStateException()
            }
        }

    private val projectKeyCache: InvalidatableCache<ProjectKey<*>?> =
        invalidatableCache(clearableInvalidatableManager) { invalidatableCache ->
            val intervalInfoRemovable = intervalInfoCache.invalidatableManager.addInvalidatable(invalidatableCache)

            when (val taskParentEntry = getProjectIdTaskParentEntry()) {
                is Schedule ->
                    InvalidatableCache.ValueHolder(taskParentEntry.projectKey) { intervalInfoRemovable.remove() }
                is RootNoScheduleOrParent ->
                    InvalidatableCache.ValueHolder(taskParentEntry.projectKey) { intervalInfoRemovable.remove() }
                is NestedTaskHierarchy -> {
                    val parentTask = taskParentEntry.parentTask as RootTask
                    val projectKey = parentTask.projectKey

                    val parentTaskRemovable = parentTask.projectKeyCache
                        .invalidatableManager
                        .addInvalidatable(invalidatableCache)

                    InvalidatableCache.ValueHolder(projectKey) {
                        intervalInfoRemovable.remove()
                        parentTaskRemovable.remove()
                    }
                }
                else -> throw IllegalStateException()
            }
        }

    fun addProjectIdInvalidatable(invalidatable: Invalidatable) =
        listOf(projectIdCache, projectKeyCache).map { it.invalidatableManager.addInvalidatable(invalidatable) }.let {
            Removable {
                it.forEach { it.remove() }
            }
        }

    private inner class NoScheduleOrParentException : Exception("task $name, $taskKey")

    override val projectId by projectIdCache

    val projectKey by projectKeyCache

    private val projectCache = invalidatableCache<OwnedProject<*>>(clearableInvalidatableManager) { invalidatableCache ->
        val projectIdRemovable = addProjectIdInvalidatable(invalidatableCache)

        val project = parent.getProject(projectId)
        val projectRemovable = project.clearableInvalidatableManager.addInvalidatable(invalidatableCache)

        InvalidatableCache.ValueHolder(project) {
            projectIdRemovable.remove()
            projectRemovable.remove()
        }
    }

    override val project by projectCache

    val noScheduleOrParentsMap = taskRecord.noScheduleOrParentRecords
        .mapValues { RootNoScheduleOrParent(this, it.value) }
        .toMutableMap()

    override val noScheduleOrParents: Collection<RootNoScheduleOrParent> get() = noScheduleOrParentsMap.values

    override val taskKey get() = TaskKey.Root(taskRecord.id)

    override val projectParentTaskHierarchies = setOf<ProjectTaskHierarchy>()

    override val allowPlaceholderCurrentNoSchedule = false

    override val projectCustomTimeIdProvider = JsonTime.ProjectCustomTimeIdProvider.rootTask

    // visible for testing
    val rootTaskDependencyResolver = RootTaskDependencyResolver(this)

    override val dependenciesLoaded get() = rootTaskDependencyResolver.dependenciesLoaded

    fun createChildTask(
        now: ExactTimeStamp.Local,
        name: String,
        note: String?,
        image: TaskJson.Image?,
        ordinal: Ordinal?,
    ): RootTask {
        ProjectRootTaskIdTracker.checkTracking()

        return parent.createTask(now, image, name, note, ordinal).apply {
            performRootIntervalUpdate { createParentNestedTaskHierarchy(this@RootTask, now) }
        }
    }

    override fun deleteFromParent() = parent.deleteRootTask(this)

    fun setName(name: String, note: String?) {
        check(name.isNotEmpty())

        taskRecord.name = name
        taskRecord.note = note

        normalizedFieldsDelegate.invalidate()
    }

    fun clearNote() {
        check(!note.isNullOrEmpty())

        taskRecord.note = null

        normalizedFieldsDelegate.invalidate()
    }

    fun setImage(deviceDbInfo: DeviceDbInfo, imageState: ImageState?) {
        taskRecord.image = when (imageState) {
            null -> null
            is ImageState.Remote -> TaskJson.Image(imageState.uuid)
            is ImageState.Local -> TaskJson.Image(imageState.uuid, deviceDbInfo.uuid)
            is ImageState.Uploading -> throw IllegalArgumentException()
        }
    }

    override fun getDateTime(instanceScheduleKey: InstanceScheduleKey) =
        DateTime(instanceScheduleKey.scheduleDate, getTime(instanceScheduleKey.scheduleTimePair))

    private fun getTime(timePair: TimePair) = timePair.customTimeKey
        ?.let { userCustomTimeProvider.getUserCustomTime(it as CustomTimeKey.User) }
        ?: Time.Normal(timePair.hourMinute!!)

    override fun getOrCopyTime(
        dayOfWeek: DayOfWeek,
        time: Time,
        customTimeMigrationHelper: OwnedProject.CustomTimeMigrationHelper,
        now: ExactTimeStamp.Local,
    ) = when (time) {
        is Time.Custom.Project<*> -> customTimeMigrationHelper.tryMigrateProjectCustomTime(time, now)
            ?: Time.Normal(time.getHourMinute(dayOfWeek))
        is Time.Custom.User -> time
        is Time.Normal -> time
    }

    fun addChild(
        childTaskRootIntervalUpdate: RootIntervalUpdate,
        now: ExactTimeStamp.Local
    ): TaskHierarchyKey {
        ProjectRootTaskIdTracker.checkTracking()

        return childTaskRootIntervalUpdate.createParentNestedTaskHierarchy(this, now)
    }

    fun deleteNoScheduleOrParent(noScheduleOrParent: NoScheduleOrParent) {
        check(noScheduleOrParentsMap.containsKey(noScheduleOrParent.id))

        noScheduleOrParentsMap.remove(noScheduleOrParent.id)
        invalidateIntervals()
    }

    override fun invalidateProjectParentTaskHierarchies() = invalidateIntervals()

    fun updateProject(projectKey: ProjectKey<*>): RootTask {
        ProjectRootTaskIdTracker.checkTracking()

        if (project.projectKey == projectKey) return this

        val interval = intervalInfo.intervals.last()

        when (val type = interval.type) {
            is Type.Schedule -> type.getParentProjectSchedule()
            is Type.NoSchedule -> type.noScheduleOrParent!!
            is Type.Child -> null // called redundantly
        }?.let {
            it.updateProject(projectKey)

            projectIdCache.invalidate()
            projectKeyCache.invalidate()
        }

        /**
         * Since the projectId depends on external factors, this call isn't 100% complete.  But the current purpose is just
         * covering an edge case to invalidate Project.rootTasksCache, and that goal is met.
         */
        rootModelChangeManager.invalidateRootTaskProjectIds()

        return this
    }

    fun createSchedules(
        now: ExactTimeStamp.Local,
        scheduleDatas: List<Pair<ScheduleData, Time>>,
        assignedTo: Set<UserKey>,
        customTimeMigrationHelper: OwnedProject.CustomTimeMigrationHelper,
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
                            projectKey.toJson(),
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
                                projectKey = projectKey.toJson(),
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
                            projectKey = projectKey.toJson(),
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
                            projectKey = projectKey.toJson(),
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
                            projectKey = projectKey.toJson(),
                        )
                    )

                    _schedules += YearlySchedule(this, yearlyScheduleRecord)
                }
            }
        }

        invalidateIntervals()
    }

    private fun Set<String>.toAssociateMap() = associate { it to true }

    private fun Time.toJson() = JsonTime.fromTime(this).toJson()

    fun copySchedules(
        now: ExactTimeStamp.Local,
        schedules: List<Schedule>,
        customTimeMigrationHelper: OwnedProject.CustomTimeMigrationHelper,
        oldProjectKey: ProjectKey<*>,
        newProjectKey: ProjectKey<*>,
    ) {
        for (schedule in schedules) {
            val today = Date.today()

            @Suppress("REDUNDANT_ELSE_IN_WHEN")
            val dayOfWeek = when (schedule) {
                is SingleSchedule -> schedule.date.dayOfWeek
                is WeeklySchedule -> schedule.dayOfWeek
                is MonthlyDaySchedule -> DayOfWeek.fromDateSoy(schedule.getDateSoyInMonth(today.year, today.month))
                is MonthlyWeekSchedule -> schedule.dayOfWeek
                is YearlySchedule -> DayOfWeek.fromDateSoy(schedule.getDateSoyInYear(today.year))
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
                            newProjectKey.toJson(),
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
                            projectKey = newProjectKey.toJson(),
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
                            projectKey = newProjectKey.toJson(),
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
                            projectKey = newProjectKey.toJson(),
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
                            projectKey = newProjectKey.toJson(),
                        )
                    )

                    _schedules += YearlySchedule(this, yearlyScheduleRecord)
                }
            }
        }

        invalidateIntervals()
    }

    fun fixProjectKeys() {
        fun ProjectIdOwner.fixProjectKey(projectId: String) = updateProject(parent.getProject(projectId).projectKey)

        schedules.forEach { it.fixProjectKey(it.projectId) }
        noScheduleOrParents.forEach { it.fixProjectKey(it.projectId) }
    }

    interface Parent : Task.Parent, OwnedProject.RootTaskProvider {

        val rootModelChangeManager: RootModelChangeManager

        fun deleteRootTask(task: RootTask)

        fun getProject(projectId: String): OwnedProject<*>

        override fun createTask(
            now: ExactTimeStamp.Local,
            image: TaskJson.Image?,
            name: String,
            note: String?,
            ordinal: Ordinal?,
        ): RootTask
    }
}
