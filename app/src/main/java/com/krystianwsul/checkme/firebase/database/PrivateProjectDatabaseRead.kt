package com.krystianwsul.checkme.firebase.database

import com.google.firebase.database.DatabaseReference
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.projects.PrivateOwnedProjectJson
import com.krystianwsul.common.utils.ProjectKey

class PrivateProjectDatabaseRead(private val projectKey: ProjectKey.Private) : TypedDatabaseRead<PrivateOwnedProjectJson>() {

    override val kClass = PrivateOwnedProjectJson::class

    override fun DatabaseReference.getQuery() = child("${DatabaseWrapper.PRIVATE_PROJECTS_KEY}/${projectKey.key}")

    override val description = "privateProject"
}