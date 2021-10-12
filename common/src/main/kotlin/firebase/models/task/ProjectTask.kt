package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.firebase.models.cache.InvalidatableCache
import com.krystianwsul.common.firebase.models.cache.invalidatableCache
import com.krystianwsul.common.firebase.models.noscheduleorparent.NoScheduleOrParent
import com.krystianwsul.common.firebase.models.noscheduleorparent.ProjectNoScheduleOrParent
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.taskhierarchy.ParentTaskDelegate
import com.krystianwsul.common.firebase.records.task.ProjectTaskRecord
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.InstanceScheduleKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.invalidatableLazy

class ProjectTask(override val project: Project<*>, private val taskRecord: ProjectTaskRecord) : Task(
    project,
    taskRecord,
    ParentTaskDelegate.Project(project),
    project.clearableInvalidatableManager,
    project.rootModelChangeManager,
) {

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

    // It's possible this is necessary for legacy data
    override val allowPlaceholderCurrentNoSchedule = true

    override val projectCustomTimeIdProvider = project.projectRecord

    private val dependenciesLoadedCache: InvalidatableCache<Boolean> =
        invalidatableCache(clearableInvalidatableManager) { invalidatableCache ->
            val customTimeKeys = taskRecord.getUserCustomTimeKeys()
            val customTimes = customTimeKeys.mapNotNull(project::tryGetUserCustomTime)

            if (customTimes.size < customTimeKeys.size) {
                val removable = rootModelChangeManager.userInvalidatableManager.addInvalidatable(invalidatableCache)

                return@invalidatableCache InvalidatableCache.ValueHolder(false) { removable.remove() }
            }

            val customTimeRemovables = customTimes.map {
                it.user
                    .clearableInvalidatableManager
                    .addInvalidatable(invalidatableCache)
            }

            InvalidatableCache.ValueHolder(true) {
                customTimeRemovables.forEach { it.remove() }
            }
        }

    override val dependenciesLoaded get() = dependenciesLoadedCache.value

    override fun deleteFromParent() = project.deleteTask(this)

    override fun getDateTime(instanceScheduleKey: InstanceScheduleKey) = project.getDateTime(instanceScheduleKey)

    override fun getOrCopyTime(
        dayOfWeek: DayOfWeek,
        time: Time,
        customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
        now: ExactTimeStamp.Local,
    ) = project.getOrCopyTime(dayOfWeek, time, customTimeMigrationHelper, now)

    fun deleteNoScheduleOrParent(noScheduleOrParent: NoScheduleOrParent) {
        check(noScheduleOrParentsMap.containsKey(noScheduleOrParent.id))

        noScheduleOrParentsMap.remove(noScheduleOrParent.id)
        invalidateIntervals()
    }

    override fun invalidateProjectParentTaskHierarchies() {
        parentProjectTaskHierarchiesProperty.invalidate()
        invalidateIntervals()
    }

    fun fixOffsets() {
        if (taskRecord.startTimeOffset == null) taskRecord.startTimeOffset = startExactTimeStamp.offset

        endData?.let {
            if (taskRecord.endData!!.offset == null) setMyEndExactTimeStamp(it)
        }

        intervalInfo.scheduleIntervals.forEach { it.schedule.fixOffsets() }
        intervalInfo.parentHierarchyIntervals.forEach { it.taskHierarchy.fixOffsets() }
        intervalInfo.noScheduleOrParentIntervals.forEach { (it.noScheduleOrParent as ProjectNoScheduleOrParent).fixOffsets() }

        existingInstances.values.forEach { it.fixOffsets() }
    }
}
