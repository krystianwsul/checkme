package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.firebase.json.noscheduleorparent.ProjectNoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.noscheduleorparent.NoScheduleOrParent
import com.krystianwsul.common.firebase.models.noscheduleorparent.ProjectNoScheduleOrParent
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

    private val noScheduleOrParentsMap = taskRecord.noScheduleOrParentRecords
        .mapValues { ProjectNoScheduleOrParent(this, it.value) }
        .toMutableMap()

    override val noScheduleOrParents: Collection<ProjectNoScheduleOrParent> get() = noScheduleOrParentsMap.values

    override val taskKey get() = TaskKey.Project(project.projectKey, taskRecord.id)

    private val parentProjectTaskHierarchiesProperty = invalidatableLazy {
        project.getTaskHierarchiesByChildTaskKey(taskKey)
    }

    override val projectParentTaskHierarchies by parentProjectTaskHierarchiesProperty

    override val projectCustomTimeIdProvider = project.projectRecord

    override fun setNoScheduleOrParent(now: ExactTimeStamp.Local, projectKey: ProjectKey<*>) {
        val noScheduleOrParentRecord =
            taskRecord.newNoScheduleOrParentRecord(ProjectNoScheduleOrParentJson(now.long, now.offset))

        check(!noScheduleOrParentsMap.containsKey(noScheduleOrParentRecord.id))

        noScheduleOrParentsMap[noScheduleOrParentRecord.id] =
            ProjectNoScheduleOrParent(this, noScheduleOrParentRecord)

        invalidateIntervals()
    }

    override fun createChildTask(
        now: ExactTimeStamp.Local,
        name: String,
        note: String?,
        image: TaskJson.Image?,
        ordinal: Double?,
    ) = project.createChildTask(this, now, name, note, image, ordinal)

    override fun deleteProjectRootTaskId() {
        // only for root projects
    }

    override fun deleteFromParent() = project.deleteTask(this)

    override fun getDateTime(scheduleKey: ScheduleKey) = project.getDateTime(scheduleKey)

    override fun getOrCopyTime(
        dayOfWeek: DayOfWeek,
        time: Time,
        customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
        now: ExactTimeStamp.Local,
    ) = project.getOrCopyTime(dayOfWeek, time, customTimeMigrationHelper, now)

    override fun addChild(childTask: Task, now: ExactTimeStamp.Local): TaskHierarchyKey {
        return project.createTaskHierarchy(this, childTask as ProjectTask, now)
    }

    fun deleteNoScheduleOrParent(noScheduleOrParent: NoScheduleOrParent) {
        check(noScheduleOrParentsMap.containsKey(noScheduleOrParent.id))

        noScheduleOrParentsMap.remove(noScheduleOrParent.id)
        invalidateIntervals()
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
        noScheduleOrParentIntervals.forEach { (it.noScheduleOrParent as ProjectNoScheduleOrParent).fixOffsets() }
        existingInstances.values.forEach { it.fixOffsets() }
    }
}
