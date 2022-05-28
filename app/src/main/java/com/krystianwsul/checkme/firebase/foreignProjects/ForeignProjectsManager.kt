package com.krystianwsul.checkme.firebase.foreignProjects

import com.krystianwsul.checkme.firebase.projects.ProjectsManager
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.projects.ForeignProjectJson
import com.krystianwsul.common.firebase.json.projects.PrivateForeignProjectJson
import com.krystianwsul.common.firebase.json.projects.SharedForeignProjectJson
import com.krystianwsul.common.firebase.managers.MapRecordManager
import com.krystianwsul.common.firebase.records.project.ForeignProjectRecord
import com.krystianwsul.common.firebase.records.project.PrivateForeignProjectRecord
import com.krystianwsul.common.firebase.records.project.SharedForeignProjectRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

class ForeignProjectsManager : ProjectsManager<ProjectType, ForeignProjectJson, ForeignProjectRecord<*>> {

    private val privateManager = object : MapRecordManager<ProjectKey.Private, PrivateForeignProjectRecord>() {

        override val databasePrefix = DatabaseWrapper.PRIVATE_PROJECTS_KEY

        override fun valueToRecord(value: PrivateForeignProjectRecord) = value

        fun set(
            projectKey: ProjectKey.Private,
            snapshot: PrivateForeignProjectJson?
        ): PrivateForeignProjectRecord? {
            return set(
                projectKey,
                { it.createObject != snapshot },
                {
                    snapshot?.let { PrivateForeignProjectRecord(projectKey, it) }
                },
            )
        }
    }

    private val sharedManager = object : MapRecordManager<ProjectKey.Shared, SharedForeignProjectRecord>() {

        // todo projectKey there's gotta be another prefix somewhere to add for jsonWrapper
        override val databasePrefix = DatabaseWrapper.RECORDS_KEY

        override fun valueToRecord(value: SharedForeignProjectRecord) = value

        fun set(
            projectKey: ProjectKey.Shared,
            json: SharedForeignProjectJson?,
        ): SharedForeignProjectRecord? {
            return set(
                projectKey,
                { it.createObject != json },
                {
                    json?.let { SharedForeignProjectRecord(projectKey, it) }
                },
            )
        }
    }

    override fun remove(projectKey: ProjectKey<*>) {
        when (projectKey) {
            is ProjectKey.Private -> privateManager.remove(projectKey)
            is ProjectKey.Shared -> sharedManager.remove(projectKey)
        }
    }

    override fun save(values: MutableMap<String, Any?>) {
        listOf(privateManager, sharedManager).forEach { it.save(values) }
    }

    override fun set(
        projectKey: ProjectKey<out ProjectType>,
        snapshot: Snapshot<out ForeignProjectJson>
    ): ForeignProjectRecord<out ProjectType>? {
        return when (projectKey) {
            is ProjectKey.Private -> privateManager.set(projectKey, snapshot.value as? PrivateForeignProjectJson)
            is ProjectKey.Shared -> sharedManager.set(projectKey, snapshot.value as? SharedForeignProjectJson)
        }
    }
}