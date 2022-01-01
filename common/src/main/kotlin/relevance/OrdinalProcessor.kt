package com.krystianwsul.common.relevance

import com.krystianwsul.common.firebase.models.ProjectOrdinalManager
import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.time.DateTimePair
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey

class OrdinalProcessor(private val users: Collection<RootUser>, private val relevantProjects: Set<Project<*>>) {

    fun process() {
        users.forEach { processProjects(it) }
    }

    private fun processProjects(user: RootUser) {
        val (relevantProjectOrdinalManagers, irrelevantProjectOrdinalManagers) =
            user.allProjectOrdinalManagers.partition { it.project in relevantProjects }

        relevantProjectOrdinalManagers.forEach(::processProject)

        // todo ordinal remove irrelevantProjectOrdinalManagers
    }

    private fun processProject(projectOrdinalManager: ProjectOrdinalManager) {
        val mutableOrdinalEntries = projectOrdinalManager.allEntries
            .map(::MutableOrdinalEntry)
            .toMutableList()

        mutableOrdinalEntries.forEach { mutableEntryKey ->
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

    private data class MutableKeyEntry(val instanceKey: InstanceKey, val instanceDateTimePair: DateTimePair) {

        constructor(immutableEntry: ProjectOrdinalManager.Key.Entry) :
                this(immutableEntry.instanceKey, immutableEntry.instanceDateTimePair)

        fun toImmutableKeyEntry(): ProjectOrdinalManager.Key.Entry {
            return ProjectOrdinalManager.Key.Entry(instanceKey, instanceDateTimePair)
        }
    }
}