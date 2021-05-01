package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.taskhierarchy.ParentTaskDelegate
import com.krystianwsul.common.firebase.records.task.ProjectTaskRecord
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.*

class ProjectTask(override val project: Project<*>, private val taskRecord: ProjectTaskRecord) :
        Task(project.copyScheduleHelper, project, taskRecord, ParentTaskDelegate.Project(project)) {

    override val parent = project

    override val taskKey get() = TaskKey.Project(project.projectKey, taskRecord.id)

    private val parentProjectTaskHierarchiesProperty = invalidatableLazy {
        project.getTaskHierarchiesByChildTaskKey(taskKey)
    }

    override val projectParentTaskHierarchies by parentProjectTaskHierarchiesProperty

    override val projectCustomTimeIdProvider = project.projectRecord

    override fun createChildTask(
            now: ExactTimeStamp.Local,
            name: String,
            note: String?,
            image: TaskJson.Image?,
            ordinal: Double?,
    ) = project.createChildTask(this, now, name, note, image, ordinal)

    override fun deleteFromParent() = project.deleteTask(this)

    override fun getDateTime(scheduleKey: ScheduleKey) = project.getDateTime(scheduleKey)

    override fun getOrCopyTime(
            ownerKey: UserKey,
            dayOfWeek: DayOfWeek,
            time: Time,
            customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
            now: ExactTimeStamp.Local,
    ) = project.getOrCopyTime(ownerKey, dayOfWeek, time, customTimeMigrationHelper, now)

    override fun addChild(childTask: Task, now: ExactTimeStamp.Local): TaskHierarchyKey {
        return project.createTaskHierarchy(this, childTask as ProjectTask, now) // todo task convert
    }

    override fun invalidateProjectParentTaskHierarchies() {
        parentProjectTaskHierarchiesProperty.invalidate()
        invalidateIntervals()
    }

    override fun updateProject(
            projectUpdater: ProjectUpdater,
            now: ExactTimeStamp.Local,
            projectKey: ProjectKey<*>,
    ) = projectUpdater.convertProject(now, this, projectKey)

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
}
