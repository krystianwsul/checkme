package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.utils.ProjectKey

abstract class SharedProjectManager :
        MapRecordManager<ProjectKey.Shared, SharedProjectRecord>(),
        SharedProjectRecord.Parent {

    override val databasePrefix = DatabaseWrapper.RECORDS_KEY

    abstract val databaseWrapper: DatabaseWrapper

    override fun valueToRecord(value: SharedProjectRecord) = value

    fun newProjectRecord(jsonWrapper: JsonWrapper) = SharedProjectRecord(
            databaseWrapper,
            this,
            jsonWrapper
    ).also { add(it.projectKey, it) }
}
