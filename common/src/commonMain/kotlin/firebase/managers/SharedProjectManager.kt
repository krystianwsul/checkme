package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.utils.ProjectKey

abstract class SharedProjectManager :
        KeyedRecordManager<ProjectKey.Shared, SharedProjectRecord>(),
        SharedProjectRecord.Parent {

    override val databasePrefix = DatabaseWrapper.RECORDS_KEY

    abstract val databaseWrapper: DatabaseWrapper

    fun newProjectRecord(jsonWrapper: JsonWrapper) = SharedProjectRecord(databaseWrapper, this, jsonWrapper).also {
        check(!records.containsKey(it.projectKey))

        records[it.projectKey] = Pair(it, false)
    }
}
