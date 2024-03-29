package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.domain.ScheduleGroup
import com.krystianwsul.common.firebase.json.noscheduleorparent.RootNoScheduleOrParentJson
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.interval.IntervalInfo
import com.krystianwsul.common.firebase.models.noscheduleorparent.RootNoScheduleOrParent
import com.krystianwsul.common.firebase.models.project.OwnedProject
import com.krystianwsul.common.firebase.models.schedule.SingleSchedule
import com.krystianwsul.common.firebase.models.taskhierarchy.NestedTaskHierarchy
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskHierarchyKey
import com.krystianwsul.common.utils.UserKey

class RootIntervalUpdate(val rootTask: RootTask, intervalInfo: IntervalInfo) :
    IntervalUpdate(rootTask, intervalInfo) {

    private data class ScheduleDiffKey(val scheduleData: ScheduleData, val assignedTo: Set<UserKey>)

    private fun getTime(timePair: TimePair) = rootTask.customTimeProvider.getTime(timePair)

    fun updateSchedules(
        shownFactory: Instance.ShownFactory,
        scheduleDatas: List<ScheduleData>,
        now: ExactTimeStamp.Local,
        assignedTo: Set<UserKey>,
        customTimeMigrationHelper: OwnedProject.CustomTimeMigrationHelper,
        projectKey: ProjectKey<*>,
        parentSingleSchedule: SingleSchedule?,
    ) {
        val removeScheduleGroups = mutableListOf<ScheduleGroup>()
        val initialAddScheduleDatas = scheduleDatas.map { ScheduleDiffKey(it, assignedTo) to it }
        val addScheduleDatas = initialAddScheduleDatas.toMutableList()

        val oldSchedules = intervalInfo.getCurrentScheduleIntervals(now).map { it.schedule }

        val oldScheduleDatas = ScheduleGroup.getGroups(oldSchedules).map {
            ScheduleDiffKey(it.scheduleData, it.assignedTo) to it
        }

        for ((scheduleDiffKey, scheduleData) in oldScheduleDatas) {
            val existing = addScheduleDatas.singleOrNull { it.first == scheduleDiffKey }

            if (existing != null)
                addScheduleDatas.remove(existing)
            else
                removeScheduleGroups += scheduleData
        }

        /*
            requirements for mock:
                there was one old schedule, it was single and mocked, and it's getting replaced
                by another single schedule
         */

        val singleRemoveReusableScheduleGroup = removeScheduleGroups.singleOrNull() as? ScheduleGroup.Reusable

        val singleAddReusableScheduleData = addScheduleDatas.singleOrNull()
            ?.second
            ?.let { it as? ScheduleData.Reusable }

        fun applyReusableScheduleData(singleSchedule: SingleSchedule, reusableScheduleData: ScheduleData.Reusable) {
            val (dateTime, parentState) = when (reusableScheduleData) {
                is ScheduleData.Single -> Pair(
                    reusableScheduleData.run { DateTime(date, getTime(timePair)) },
                    Instance.ParentState.NoParent,
                )
                is ScheduleData.Child -> reusableScheduleData.parentInstanceKey.let {
                    Pair(
                        rootTask.parent
                            .getInstance(it)
                            .instanceDateTime,
                        Instance.ParentState.Parent(it),
                    )
                }
            }

            singleSchedule.getInstance(rootTask).let {
                it.setInstanceDateTime(
                    shownFactory,
                    dateTime,
                    customTimeMigrationHelper,
                    now,
                )

                it.setParentState(parentState)
            }
        }

        if (singleRemoveReusableScheduleGroup != null && singleAddReusableScheduleData != null) {
            val singleSchedule = singleRemoveReusableScheduleGroup.singleSchedule
            // This is for a regular single-instance reminder, and applies the schedule change to the instance.

            singleSchedule.setAssignedTo(assignedTo)

            applyReusableScheduleData(singleSchedule, singleAddReusableScheduleData)
        } else if (parentSingleSchedule != null && singleAddReusableScheduleData != null) {
            check(removeScheduleGroups.isEmpty())

            /*
            In this scenario, we create a schedule that matches the parent's original one, and then manipulate the instance
            time.  That way, we get to keep the previous instance, along with its children's done states.
             */

            rootTask.createSchedules(
                now,
                parentSingleSchedule.originalScheduleDateTime.run { listOf(ScheduleData.Single(date, time.timePair)) },
                assignedTo,
                customTimeMigrationHelper,
                projectKey,
            )

            applyReusableScheduleData(parentSingleSchedule, singleAddReusableScheduleData)
        } else {
            removeScheduleGroups.asSequence()
                .flatMap { it.schedules }
                .forEach { it.setEndExactTimeStamp(now.toOffset()) }

            rootTask.createSchedules(
                now,
                addScheduleDatas.map { it.second },
                assignedTo,
                customTimeMigrationHelper,
                projectKey,
            )
        }
    }

    fun setNoScheduleOrParent(now: ExactTimeStamp.Local, projectKey: ProjectKey<*>) {
        val noScheduleOrParentRecord = rootTask.taskRecord.newNoScheduleOrParentRecord(
            RootNoScheduleOrParentJson(
                now.long,
                now.offset,
                projectId = projectKey.key,
                projectKey = projectKey.toJson(),
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