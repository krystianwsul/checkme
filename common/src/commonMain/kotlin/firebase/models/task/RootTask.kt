package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.CopyScheduleHelper
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.taskhierarchy.NestedTaskHierarchy
import com.krystianwsul.common.firebase.models.taskhierarchy.ParentTaskDelegate
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*

class RootTask(
        val taskRecord: RootTaskRecord,
        private val parent: Parent,
        private val userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
) : Task(
        CopyScheduleHelper.Root,
        JsonTime.CustomTimeProvider.getForRootTask(userCustomTimeProvider),
        taskRecord,
        ParentTaskDelegate.Root(parent),
) {

    /**
     * todo task project this is kinda insane, but I'm going to cross my fingers that the projectKey won't be immediately
     * necessary when creating the object.  I'm going to initialize it AFTER the project is initialized.  Probably
     * build this out to a delegate that does book-keeping on whether or not the projectKey is valid (init vs. editing)
     *
     * It feels iffy, but less iffy than passing in the initial projectKey and keeping track of it beforehand.  Ideally,
     * I'd pass the project into every call that involves the current project, but that would require wrapping the task
     * into some sort of object that holds the current project and delegates calls to the task, and that would require
     * a shit-ton of work.
     */
    lateinit var projectKey: ProjectKey<*>

    private val projectProperty = invalidatableLazy { parent.getProject(projectKey) }
    override val project by projectProperty

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
    ) = TODO("todo task edit")

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

        fun getProject(projectKey: ProjectKey<*>): Project<*>
    }
}
