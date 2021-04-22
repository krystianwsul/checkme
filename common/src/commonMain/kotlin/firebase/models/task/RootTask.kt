package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.CopyScheduleHelper
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.taskhierarchy.NestedTaskHierarchy
import com.krystianwsul.common.firebase.models.taskhierarchy.ParentTaskDelegate
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.records.task.ProjectTaskRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*

class RootTask(
        project: Project<*>,
        private val taskRecord: ProjectTaskRecord,
        private val parent: Parent,
        private val userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
) : Task(
        project,
        CopyScheduleHelper.Root,
        JsonTime.CustomTimeProvider.getForRootTask(userCustomTimeProvider),
        taskRecord,
        ParentTaskDelegate.Root(parent),
) {

    override val taskKey get() = TaskKey.Root(taskRecord.id)

    override val projectParentTaskHierarchies = setOf<ProjectTaskHierarchy>()

    private val childHierarchyIntervalsProperty = invalidatableLazy {
        parent.getTaskHierarchiesByParentTaskKey(taskKey)
                .map { it.childTask }
                .distinct()
                .flatMap { it.parentHierarchyIntervals }
                .filter { it.taskHierarchy.parentTaskKey == taskKey }
    }
    override val childHierarchyIntervals by childHierarchyIntervalsProperty

    override fun createChildTask(
            now: ExactTimeStamp.Local,
            name: String,
            note: String?,
            image: TaskJson.Image?,
            ordinal: Double?,
    ) = TODO("todo task fetch")

    override fun deleteFromParent() = parent.deleteTask(this)

    override fun getDateTime(scheduleKey: ScheduleKey) =
            DateTime(scheduleKey.scheduleDate, getTime(scheduleKey.scheduleTimePair))

    private fun getTime(timePair: TimePair) = timePair.customTimeKey
            ?.let { userCustomTimeProvider.getUserCustomTime(it as CustomTimeKey.User) }
            ?: Time.Normal(timePair.hourMinute!!)

    override fun getOrCopyTime(
            ownerKey: UserKey,
            dayOfWeek: DayOfWeek,
            time: Time,
            customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
            now: ExactTimeStamp.Local,
    ) = when (time) {
        is Time.Custom.Project<*> -> {
            check(Time.Custom.User.WRITE_USER_CUSTOM_TIMES)

            customTimeMigrationHelper.tryMigrateProjectCustomTime(time, now)
                    ?: Time.Normal(time.getHourMinute(dayOfWeek))
        }
        is Time.Custom.User -> time
        is Time.Normal -> time
    }

    override fun addChild(childTask: Task, now: ExactTimeStamp.Local) =
            createParentNestedTaskHierarchy(childTask, now)

    override fun invalidateProjectParentTaskHierarchies() = invalidateIntervals()

    override fun invalidateChildTaskHierarchies() = childHierarchyIntervalsProperty.invalidate()

    override fun updateProject(
            projectUpdater: ProjectUpdater,
            now: ExactTimeStamp.Local,
            projectId: ProjectKey<*>,
    ): Task {
        return if (projectId == project.projectKey)
            this
        else
            projectUpdater.convert(now, this, projectId)
    }

    fun fixOffsets() {
        if (taskRecord.startTimeOffset == null) taskRecord.startTimeOffset = startExactTimeStamp.offset

        endData?.let {
            if (taskRecord.endData!!.offset == null) setMyEndExactTimeStamp(it)
        }

        scheduleIntervals.forEach { it.schedule.fixOffsets() }
        parentHierarchyIntervals.forEach { it.taskHierarchy.fixOffsets() }
        noScheduleOrParentIntervals.forEach { it.noScheduleOrParent.fixOffsets() }
        existingInstances.values.forEach { it.fixOffsets() }
    }

    interface Parent {

        fun getTaskHierarchiesByParentTaskKey(childTaskKey: TaskKey.Root): Set<NestedTaskHierarchy>

        fun deleteTask(task: RootTask)

        fun getTask(taskKey: TaskKey.Root): RootTask
    }
}
