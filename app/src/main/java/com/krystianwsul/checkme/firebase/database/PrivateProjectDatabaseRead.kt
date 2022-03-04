package com.krystianwsul.checkme.firebase.database

import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.utils.ProjectKey

class PrivateProjectDatabaseRead(private val projectKey: ProjectKey.Private) : TypedDatabaseRead<PrivateProjectJson>() {

    override val type = "privateProject"

    override val kClass = PrivateProjectJson::class

    override fun getResult() =
        AndroidDatabaseWrapper.rootReference.child("${DatabaseWrapper.PRIVATE_PROJECTS_KEY}/${projectKey.key}")
            .typedSnapshotChanges()
}