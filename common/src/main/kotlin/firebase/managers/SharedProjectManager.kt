package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.records.project.SharedOwnedProjectRecord
import com.krystianwsul.common.utils.ProjectKey

abstract class SharedProjectManager(private val databaseWrapper: DatabaseWrapper) :
    MapRecordManager<ProjectKey.Shared, SharedOwnedProjectRecord>(),
    SharedOwnedProjectRecord.Parent {

    override val databasePrefix = DatabaseWrapper.RECORDS_KEY

    override fun valueToRecord(value: SharedOwnedProjectRecord) = value

    fun newProjectRecord(jsonWrapper: JsonWrapper) = SharedOwnedProjectRecord(
        databaseWrapper,
        this,
        jsonWrapper
    ).also { add(it.projectKey, it) }
}
