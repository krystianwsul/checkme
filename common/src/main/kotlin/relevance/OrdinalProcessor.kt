package com.krystianwsul.common.relevance

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.project.SharedOwnedProject
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.users.ProjectOrdinalManager
import com.krystianwsul.common.firebase.models.users.RootUser
import com.krystianwsul.common.time.DateOrDayOfWeek
import com.krystianwsul.common.time.DateTimePair
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.InstanceScheduleKey
import com.krystianwsul.common.utils.Ordinal
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey

class OrdinalProcessor(
    private val users: Collection<RootUser>,
    private val relevantProjects: Map<ProjectKey.Shared, SharedOwnedProject>,
    private val relevantTasks: Map<TaskKey, Task>,
    private val customTimeRelevanceCollection: CustomTimeRelevanceCollection,
    private val now: ExactTimeStamp.Local,
) {

    private val oldestVisibleTaskDates = relevantTasks.mapValues { (_, task) ->
        task.getInstances(
            null,
            null,
            now,
            filterVisible = false,
        )
            .firstOrNull()
            ?.instanceDate
    }

    private val oldestVisibleProjectDates = relevantProjects.mapValues { (_, project) ->
        project.getAllDependenciesLoadedTasks()
            .asSequence()
            .mapNotNull { oldestVisibleTaskDates[it.taskKey] }
            .minOrNull()
    }

    fun process() {
        users.forEach { processProjects(it) }
    }

    private fun processProjects(user: RootUser) {
        user.projectIds
            .mapNotNull { relevantProjects[it] }
            .forEach {
                user.remoteRootUserRecord.setOrdinalEntries(
                    it.projectKey,
                    processProject(it, user.getOrdinalEntriesForProject(it)).mapValues { it.value.toJson() }
                )
            }
    }

    private fun processProject(
        project: SharedOwnedProject,
        ordinalEntries: Map<String, ProjectOrdinalManager.OrdinalEntry>,
    ): Map<String, ProjectOrdinalManager.OrdinalEntry> {
        val mutableOrdinalEntries = ordinalEntries.mapValues { MutableOrdinalEntry(it.value) }

        mutableOrdinalEntries.values.forEach { mutableOrdinalEntry ->
            // pruning

            mutableOrdinalEntry.mutableKeyEntries.forEach { mutableKeyEntry ->
                mutableKeyEntry.mutableTaskInfo?.let {
                    val task = relevantTasks[it.taskKey]

                    if (task == null) {
                        mutableKeyEntry.mutableTaskInfo = null
                    } else if (it.scheduleDateTimePair != null) {
                        val instanceScheduleKey =
                            InstanceScheduleKey(it.scheduleDateTimePair!!.date, it.scheduleDateTimePair!!.timePair)

                        val isVisible = task.getInstance(instanceScheduleKey).isVisible(
                            now,
                            Instance.VisibilityOptions(hack24 = true),
                        )

                        if (!isVisible) {
                            mutableKeyEntry.instanceDateOrDayOfWeek
                                .date
                                ?.let { mutableKeyEntry.instanceDateOrDayOfWeek = DateOrDayOfWeek.DayOfWeek(it.dayOfWeek) }

                            mutableKeyEntry.mutableTaskInfo!!.scheduleDateTimePair = null
                        }
                    }
                }

                mutableKeyEntry.instanceDateOrDayOfWeek.date?.let { instanceDate ->
                    val oldestVisibleDate = oldestVisibleProjectDates.getValue(project.projectKey)

                    if (oldestVisibleDate == null || instanceDate < oldestVisibleDate)
                        mutableKeyEntry.instanceDateOrDayOfWeek = DateOrDayOfWeek.DayOfWeek(instanceDate.dayOfWeek)
                }

                mutableKeyEntry.instanceTimePair
                    .customTimeKey
                    ?.let { instanceCustomTimeKey ->
                        val customTimeRelevance = customTimeRelevanceCollection.relevances.getValue(instanceCustomTimeKey)

                        if (!customTimeRelevance.relevant) {
                            mutableKeyEntry.instanceTimePair = TimePair(
                                customTimeRelevance.customTime.getHourMinute(
                                    mutableKeyEntry.instanceDateOrDayOfWeek.dayOfWeek
                                )
                            )
                        }
                    }
            }
        }

        return mutableOrdinalEntries.mapValues {
            ProjectOrdinalManager.OrdinalEntry(it.value.getImmutableKeys(), it.value.getImmutableValue())
        }
            .entries
            .groupBy { it.value.key }
            .entries
            .map { (_, ordinalEntries) ->
                ordinalEntries.maxByOrNull { it.value.value.updated }!!
            }
            .associate { it.key to it.value }
    }

    private data class MutableOrdinalEntry(
        val mutableKeyEntries: MutableList<MutableKeyEntry>,
        val ordinal: Ordinal,
        val updated: ExactTimeStamp.Local,
        val parentState: ProjectOrdinalManager.Key.ParentState,
    ) {

        constructor(ordinalEntry: ProjectOrdinalManager.OrdinalEntry) : this(
            ordinalEntry.key
                .entries
                .map(::MutableKeyEntry)
                .toMutableList(),
            ordinalEntry.value.ordinal,
            ordinalEntry.value.updated,
            ordinalEntry.key.parentState,
        )

        fun getImmutableKeys() =
            ProjectOrdinalManager.Key(mutableKeyEntries.map { it.toImmutableKeyEntry() }.toSet(), parentState)

        fun getImmutableValue() = ProjectOrdinalManager.Value(ordinal, updated)
    }

    private data class MutableKeyEntry(
        var mutableTaskInfo: MutableTaskInfo?,
        var instanceDateOrDayOfWeek: DateOrDayOfWeek,
        var instanceTimePair: TimePair,
    ) {

        constructor(immutableEntry: ProjectOrdinalManager.Key.Entry) : this(
            immutableEntry.taskInfo?.let(::MutableTaskInfo),
            immutableEntry.instanceDateOrDayOfWeek,
            immutableEntry.instanceTimePair,
        )

        fun toImmutableKeyEntry(): ProjectOrdinalManager.Key.Entry {
            return ProjectOrdinalManager.Key.Entry(
                mutableTaskInfo?.toImmutableTaskInfo(),
                instanceDateOrDayOfWeek,
                instanceTimePair,
            )
        }

        data class MutableTaskInfo(val taskKey: TaskKey, var scheduleDateTimePair: DateTimePair?) {

            constructor(taskInfo: ProjectOrdinalManager.Key.TaskInfo) : this(taskInfo.taskKey, taskInfo.scheduleDateTimePair)

            fun toImmutableTaskInfo() = ProjectOrdinalManager.Key.TaskInfo(taskKey, scheduleDateTimePair)
        }
    }
}