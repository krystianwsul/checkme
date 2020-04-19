package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.utils.ProjectKey

abstract class SharedProjectManager : SharedProjectRecord.Parent {

    val isSaved get() = sharedProjectRecords.any { it.value.second }

    var sharedProjectRecords = mutableMapOf<ProjectKey.Shared, Pair<SharedProjectRecord, Boolean>>()
        private set

    abstract val databaseWrapper: DatabaseWrapper

    fun save(values: MutableMap<String, Any?>) {
        val myValues = mutableMapOf<String, Any?>()

        val newRemoteProjectRecords = sharedProjectRecords.mapValues {
            Pair(it.value.first, it.value.first.getValues(myValues))
        }.toMutableMap()

        ErrorLogger.instance.log("RemoteSharedProjectManager.save values: $myValues")

        if (myValues.isNotEmpty()) {
            check(!isSaved)

            sharedProjectRecords = newRemoteProjectRecords

            values += myValues.mapKeys { "${DatabaseWrapper.RECORDS_KEY}/${it.key}" }
        }
    }

    fun newProjectRecord(jsonWrapper: JsonWrapper) = SharedProjectRecord(databaseWrapper, this, jsonWrapper).also {
        check(!sharedProjectRecords.containsKey(it.projectKey))

        setSharedProjectRecord(it.projectKey, Pair(it, false))
    }

    abstract fun setSharedProjectRecord(
            projectKey: ProjectKey.Shared,
            pair: Pair<SharedProjectRecord, Boolean>
    )
}
