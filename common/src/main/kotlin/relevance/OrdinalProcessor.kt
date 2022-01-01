package com.krystianwsul.common.relevance

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.ProjectOrdinalManager
import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.time.DateOrDayOfWeek
import com.krystianwsul.common.time.DateTimePair
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.InstanceScheduleKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey

class OrdinalProcessor(
    private val users: Collection<RootUser>,
    private val relevantProjects: Map<ProjectKey<*>, Project<*>>,
    private val relevantTasks: Map<TaskKey, Task>,
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
            user.allProjectOrdinalManagers.partition { it.project.projectKey in relevantProjects }

        relevantProjectOrdinalManagers.forEach(::processProject)

        // todo ordinal remove irrelevantProjectOrdinalManagers
    }

    private fun processProject(projectOrdinalManager: ProjectOrdinalManager) {
        val mutableOrdinalEntries = projectOrdinalManager.allEntries
            .map(::MutableOrdinalEntry)
            .toMutableList()

        mutableOrdinalEntries.forEach { mutableOrdinalEntry ->
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

                        if (!isVisible) mutableKeyEntry.mutableTaskInfo!!.scheduleDateTimePair = null
                    }
                }
            }
        }

        // todo ordinal remove
        val immutableEntries = mutableOrdinalEntries.associate { it.toImmutableEntryPair() }
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

        fun toImmutableEntryPair(): Pair<ProjectOrdinalManager.Key, ProjectOrdinalManager.Value> {
            return Pair(
                ProjectOrdinalManager.Key(mutableKeyEntries.map { it.toImmutableKeyEntry() }.toSet()),
                ProjectOrdinalManager.Value(ordinal, updated)
            )
        }
    }

    private data class MutableKeyEntry(
        var mutableTaskInfo: MutableTaskInfo?,
        val instanceDateOrDayOfWeek: DateOrDayOfWeek,
        val instanceTimePair: TimePair,
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