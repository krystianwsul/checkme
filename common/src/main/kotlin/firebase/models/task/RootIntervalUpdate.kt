package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.firebase.json.noscheduleorparent.RootNoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.interval.IntervalInfo
import com.krystianwsul.common.firebase.models.noscheduleorparent.RootNoScheduleOrParent
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.schedule.Schedule
import com.krystianwsul.common.firebase.models.schedule.SingleSchedule
import com.krystianwsul.common.firebase.models.taskhierarchy.NestedTaskHierarchy
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.UserKey

class RootIntervalUpdate(val rootTask: RootTask, intervalInfo: IntervalInfo) :
    IntervalUpdate(rootTask, intervalInfo) {

    private data class ScheduleDiffKey(val scheduleData: ScheduleData, val assignedTo: Set<UserKey>)

    fun updateSchedules(
        shownFactory: Instance.ShownFactory,
        scheduleDatas: List<Pair<ScheduleData, Time>>,
        now: ExactTimeStamp.Local,
        assignedTo: Set<UserKey>,
        customTimeMigrationHelper: Project.CustomTimeMigrationHelper,
        projectKey: ProjectKey<*>,
        parentSingleSchedule: SingleSchedule? = null,
    ) {
        val removeSchedules = mutableListOf<Schedule>()
        val initialAddScheduleDatas = scheduleDatas.map { ScheduleDiffKey(it.first, assignedTo) to it }
        val addScheduleDatas = initialAddScheduleDatas.toMutableList()

        val oldSchedules = intervalInfo.getCurrentScheduleIntervals(now).map { it.schedule }

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

        val singleAddSchedulePair = addScheduleDatas.singleOrNull()
            ?.second
            ?.let { (scheduleData, time) ->
                scheduleData.let { it as? ScheduleData.Single }?.let { it to time }
            }

        fun SingleSchedule.setTimeOnInstance(singleScheduleData: ScheduleData.Single, time: Time) {
            if (assignedTo.isNotEmpty()) setAssignedTo(assignedTo)

            getInstance(rootTask).let {
                it.setInstanceDateTime(
                    shownFactory,
                    DateTime(singleScheduleData.date, time),
                    customTimeMigrationHelper,
                    now,
                )

                it.setParentState(Instance.ParentState.NoParent)
            }
        }

        if (singleRemoveSchedule != null && singleAddSchedulePair != null) {
            // todo hierarchy later: schedule checks
            // check(parentSingleSchedule == null)

            singleRemoveSchedule.setTimeOnInstance(singleAddSchedulePair.first, singleAddSchedulePair.second)

            if (assignedTo.isNotEmpty()) singleRemoveSchedule.setAssignedTo(assignedTo)

            singleRemoveSchedule.getInstance(rootTask).let {
                it.setInstanceDateTime(
                    shownFactory,
                    singleAddSchedulePair.run { DateTime(first.date, second) },
                    customTimeMigrationHelper,
                    now,
                )

                it.setParentState(Instance.ParentState.NoParent)
            }
        } else if (parentSingleSchedule != null && singleAddSchedulePair != null) {
            check(removeSchedules.isEmpty())

            /*
            In this scenario, we create a schedule that matches the parent's original one, and then manipulate the instance
            time.  That way, we get to keep the previous instance, along with its children's done states.
             */
            rootTask.createSchedules(
                now,
                parentSingleSchedule.originalScheduleDateTime.let {
                    listOf(ScheduleData.Single(it.date, it.time.timePair) to it.time)
                },
                assignedTo,
                customTimeMigrationHelper,
                projectKey,
            )

            parentSingleSchedule.getInstance(rootTask).let {
                it.setInstanceDateTime(
                    shownFactory,
                    singleAddSchedulePair.run { DateTime(first.date, second) },
                    customTimeMigrationHelper,
                    now,
                )

                it.setParentState(Instance.ParentState.NoParent)
            }
        } else {
            if (addScheduleDatas.isEmpty()) {
                // hierarchy hack

                check(removeSchedules.isEmpty())
                check(initialAddScheduleDatas.size == 1)
                check(oldScheduleDatas.size == 1)

                val singleInitialAddSchedulePair = initialAddScheduleDatas.single().second

                val singleAddSchedule = singleInitialAddSchedulePair.first.let { it as ScheduleData.Single }

                val singleOldSchedule = oldScheduleDatas.single()
                    .second
                    .single()
                    .let { it as SingleSchedule }

                singleOldSchedule.setTimeOnInstance(singleAddSchedule, singleInitialAddSchedulePair.second)
            } else {
                check(addScheduleDatas.isNotEmpty())

                removeSchedules.forEach { it.setEndExactTimeStamp(now.toOffset()) }

                rootTask.createSchedules(
                    now,
                    addScheduleDatas.map { it.second },
                    assignedTo,
                    customTimeMigrationHelper,
                    projectKey,
                )
            }
        }
    }

    fun setNoScheduleOrParent(now: ExactTimeStamp.Local, projectKey: ProjectKey<*>) {
        val noScheduleOrParentRecord = rootTask.taskRecord.newNoScheduleOrParentRecord(
            RootNoScheduleOrParentJson(
                now.long,
                now.offset,
                projectId = projectKey.key,
            )
        )

        check(!rootTask.noScheduleOrParentsMap.containsKey(noScheduleOrParentRecord.id))

        rootTask.noScheduleOrParentsMap[noScheduleOrParentRecord.id] =
            RootNoScheduleOrParent(rootTask, noScheduleOrParentRecord)

        invalidateIntervals()
    }

    fun createParentNestedTaskHierarchy(parentTask: Task, now: ExactTimeStamp.Local): TaskHierarchyKey.Nested {
        val taskHierarchyJson = NestedTaskHierarchyJson(parentTask.id, now.long, now.offset)

        return createParentNestedTaskHierarchy(taskHierarchyJson).taskHierarchyKey
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

    private fun createParentNestedTaskHierarchy(nestedTaskHierarchyJson: NestedTaskHierarchyJson): NestedTaskHierarchy {
        val taskHierarchyRecord = rootTask.taskRecord.newTaskHierarchyRecord(nestedTaskHierarchyJson)
        val taskHierarchy = NestedTaskHierarchy(rootTask, taskHierarchyRecord, rootTask.parentTaskDelegateFactory)

        rootTask.nestedParentTaskHierarchies[taskHierarchy.id] = taskHierarchy

        taskHierarchy.invalidateTasks()

        return taskHierarchy
    }
}