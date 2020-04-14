package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.utils.ProjectKey

abstract class SharedProjectManager<T> : SharedProjectRecord.Parent {

    val isSaved get() = sharedProjectRecords.any { it.value.second }

    var sharedProjectRecords = mutableMapOf<ProjectKey.Shared, Pair<SharedProjectRecord, Boolean>>()
        private set

    abstract val databaseWrapper: DatabaseWrapper

    protected abstract fun getDatabaseCallback(extra: T): DatabaseCallback

    open val saveCallback: (() -> Unit)? = null

    fun save(extra: T): Boolean {
        val values = mutableMapOf<String, Any?>()

        val newRemoteProjectRecords = sharedProjectRecords.mapValues {
            Pair(it.value.first, it.value.first.getValues(values))
        }.toMutableMap()

        ErrorLogger.instance.log("RemoteSharedProjectManager.save values: $values")

        if (values.isNotEmpty()) {
            check(!isSaved)

            sharedProjectRecords = newRemoteProjectRecords

            databaseWrapper.updateRecords(values, getDatabaseCallback(extra))
        } else {
            saveCallback?.invoke()
        }

        return isSaved
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
