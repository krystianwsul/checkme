package com.krystianwsul.common.relevance

import com.krystianwsul.common.firebase.models.ProjectOrdinalManager
import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.models.project.Project

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
        val originalEntries = projectOrdinalManager.allEntries

        originalEntries.forEach { (key, value) ->
            key.entries.forEach { keyEntry ->
                val taskKey = keyEntry.instanceKey.taskKey
            }
        }
    }
}