package com.krystianwsul.common.relevance

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.users.ProjectOrdinalManager
import com.krystianwsul.common.firebase.models.users.RootUser
import com.krystianwsul.common.time.DateOrDayOfWeek
import com.krystianwsul.common.time.DateTimePair
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.InstanceScheduleKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey

class OrdinalProcessor(
    private val users: Collection<RootUser>,
    private val relevantProjects: Map<ProjectKey.Shared, SharedProject>,
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
        val (relevantProjectOrdinalManagers, irrelevantProjectOrdinalManagers) =
            user.allProjectOrdinalManagers.partition {
                it.projectKey.let { it in relevantProjects && it in user.projectIds }
            }

        relevantProjectOrdinalManagers.forEach(::processProject)

        // todo ordinal remove irrelevantProjectOrdinalManagers
    }

    private fun processProject(projectOrdinalManager: ProjectOrdinalManager) {
        val mutableOrdinalEntries = projectOrdinalManager.allEntries
            .map(::MutableOrdinalEntry)
            .toMutableList()

        mutableOrdinalEntries.forEach { mutableOrdinalEntry ->
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
                    val oldestVisibleDate = oldestVisibleProjectDates.getValue(projectOrdinalManager.projectKey)

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

        // todo ordinal store
        val immutableEntries = mutableOrdinalEntries.groupBy { it.getImmutableKeys() }
            .mapValues {
                it.value
                    .map { it.getImmutableValue() }
                    .maxByOrNull { it.updated }!!
            }
    }

    private data class MutableOrdinalEntry(
        val mutableKeyEntries: MutableList<MutableKeyEntry>,
        val ordinal: Double,
        val updated: ExactTimeStamp.Local,
    ) {

        constructor(pair: Pair<ProjectOrdinalManager.Key, ProjectOrdinalManager.Value>) : this(
            pair.first
                .entries
                .map(::MutableKeyEntry)
                .toMutableList(),
            pair.second.ordinal,
            pair.second.updated,
        )

        fun getImmutableKeys() = ProjectOrdinalManager.Key(mutableKeyEntries.map { it.toImmutableKeyEntry() }.toSet())

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

        // todo ordinal strip out scheduleDateTimePair instead of taskKey
        data class MutableTaskInfo(val taskKey: TaskKey, var scheduleDateTimePair: DateTimePair?) {

            constructor(taskInfo: ProjectOrdinalManager.Key.TaskInfo) : this(taskInfo.taskKey, taskInfo.scheduleDateTimePair)

            fun toImmutableTaskInfo() = ProjectOrdinalManager.Key.TaskInfo(taskKey, scheduleDateTimePair)
        }
    }
}