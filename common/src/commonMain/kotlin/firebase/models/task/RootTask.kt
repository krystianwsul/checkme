package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.firebase.json.noscheduleorparent.RootNoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.CopyScheduleHelper
import com.krystianwsul.common.firebase.models.interval.Type
import com.krystianwsul.common.firebase.models.noscheduleorparent.NoScheduleOrParent
import com.krystianwsul.common.firebase.models.noscheduleorparent.RootNoScheduleOrParent
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.taskhierarchy.ParentTaskDelegate
import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*

class RootTask(
    val taskRecord: RootTaskRecord,
    override val parent: Parent,
    private val userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
) : Task(
    CopyScheduleHelper.Root,
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

    fun addChild(childTask: Task, now: ExactTimeStamp.Local): TaskHierarchyKey {
        val taskHierarchyKey = childTask.createParentNestedTaskHierarchy(this, now)
        addRootTask(childTask as RootTask)

        return taskHierarchyKey
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
