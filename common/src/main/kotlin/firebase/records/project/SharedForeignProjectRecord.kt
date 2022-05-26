package com.krystianwsul.common.firebase.records.project

import com.krystianwsul.common.firebase.json.projects.SharedForeignProjectJson
import com.krystianwsul.common.firebase.records.users.ForeignProjectUserRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

class SharedForeignProjectRecord(
    override val projectKey: ProjectKey.Shared,
    private val projectJson: SharedForeignProjectJson,
) : ForeignProjectRecord<ProjectType.Shared>(
    projectJson,
    projectKey,
    "${projectKey.key}/$PROJECT_JSON",
), SharedProjectRecord {

    override val name get() = projectJson.name

    override val userRecords =
        SharedProjectRecord.parseUserJsons(projectJson.users) { ForeignProjectUserRecord(this, it) }

    override val children get() = super.children + userRecords.values

    override val childKey get() = "$key/$PROJECT_JSON"
}