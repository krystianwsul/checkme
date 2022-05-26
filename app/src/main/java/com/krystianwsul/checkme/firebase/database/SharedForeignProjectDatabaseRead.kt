package com.krystianwsul.checkme.firebase.database

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.core.Path
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.projects.SharedForeignProjectJson
import com.krystianwsul.common.utils.ProjectKey

class SharedForeignProjectDatabaseRead(private val projectKey: ProjectKey.Shared) :
    TypedDatabaseRead<SharedForeignProjectJson>() {

    override val kClass = SharedForeignProjectJson::class

    override fun pathToPaperKey(path: Path) = "foreign/" + super.pathToPaperKey(path)

    override fun DatabaseReference.getQuery() =
        child("${DatabaseWrapper.RECORDS_KEY}/${projectKey.key}/${JsonWrapper.PROJECT_JSON}")

    override val description = "sharedForeignProject"
}