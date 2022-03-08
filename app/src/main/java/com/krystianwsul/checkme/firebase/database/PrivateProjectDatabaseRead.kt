package com.krystianwsul.checkme.firebase.database

import com.google.firebase.database.DatabaseReference
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.utils.ProjectKey

class PrivateProjectDatabaseRead(private val projectKey: ProjectKey.Private) : TypedDatabaseRead<PrivateProjectJson>() {

    override val type = "privateProject"

    override val key get() = projectKey.toString()

    override val log = true

    override val kClass = PrivateProjectJson::class

    override fun DatabaseReference.getQuery() = child("${DatabaseWrapper.PRIVATE_PROJECTS_KEY}/${projectKey.key}")
}