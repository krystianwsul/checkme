package com.krystianwsul.checkme.firebase.database

import com.google.firebase.database.DatabaseReference
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.utils.ProjectKey

class SharedProjectDatabaseRead(private val projectKey: ProjectKey.Shared) : TypedDatabaseRead<JsonWrapper>() {

    override val kClass = JsonWrapper::class

    override fun DatabaseReference.getQuery() = child("${DatabaseWrapper.RECORDS_KEY}/${projectKey.key}")
}