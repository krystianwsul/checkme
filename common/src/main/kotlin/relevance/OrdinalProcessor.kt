package com.krystianwsul.common.relevance

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.ProjectOrdinalManager
import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.time.DateTimePair
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
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
                mutableKeyEntry.instanceKey?.let {
                    if (
                        relevantTasks[it.taskKey]?.getInstance(it.instanceScheduleKey)?.isVisible(
                            now,
                            Instance.VisibilityOptions(hack24 = true),
                        ) != true
                    ) {
                        mutableKeyEntry.instanceKey = null
                    }
                }
            }
        }

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

    private data class MutableKeyEntry(var instanceKey: InstanceKey?, val instanceDateTimePair: DateTimePair) {

        constructor(immutableEntry: ProjectOrdinalManager.Key.Entry) :
                this(immutableEntry.instanceKey, immutableEntry.instanceDateTimePair)

        fun toImmutableKeyEntry(): ProjectOrdinalManager.Key.Entry {
            return ProjectOrdinalManager.Key.Entry(instanceKey, instanceDateTimePair)
        }
    }

    sealed class DateOrDayOfWeek {

        data class Date(val date: com.krystianwsul.common.time.Date) : DateOrDayOfWeek()

        data class DayOfWeek(val dayOfWeek: com.krystianwsul.common.time.DayOfWeek) : DateOrDayOfWeek()
    }
}