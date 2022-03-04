package com.krystianwsul.checkme.firebase.database

import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.utils.ProjectKey

class SharedProjectDatabaseRead(private val projectKey: ProjectKey.Shared) : TypedDatabaseRead<JsonWrapper>() {

    override val type = "sharedProject"

    override val kClass = JsonWrapper::class

    override fun getResult() =
        AndroidDatabaseWrapper.rootReference.child("${DatabaseWrapper.RECORDS_KEY}/${projectKey.key}").typedSnapshotChanges()
}