package com.krystianwsul.checkme.firebase.database

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.core.Path
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.projects.PrivateForeignProjectJson
import com.krystianwsul.common.utils.ProjectKey

class PrivateForeignProjectDatabaseRead(private val projectKey: ProjectKey.Private) :
    TypedDatabaseRead<PrivateForeignProjectJson>() {

    override val kClass = PrivateForeignProjectJson::class

    override fun pathToPaperKey(path: Path) = "foreign/" + super.pathToPaperKey(path)

    override fun DatabaseReference.getQuery() = child("${DatabaseWrapper.PRIVATE_PROJECTS_KEY}/${projectKey.key}")

    override val description = "privateForeignProject"
}