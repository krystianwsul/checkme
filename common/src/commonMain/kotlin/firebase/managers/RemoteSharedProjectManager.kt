package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.records.RemoteSharedProjectRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

abstract class RemoteSharedProjectManager<T> : RemoteSharedProjectRecord.Parent {

    val isSaved get() = remoteProjectRecords.any { it.value.second }

    abstract var remoteProjectRecords: MutableMap<ProjectKey.Shared, Pair<RemoteSharedProjectRecord, Boolean>>
        protected set

    abstract val databaseWrapper: DatabaseWrapper

    protected abstract fun getDatabaseCallback(extra: T): DatabaseCallback

    open val saveCallback: (() -> Unit)? = null

    fun save(extra: T): Boolean {
        val values = mutableMapOf<String, Any?>()

        val newRemoteProjectRecords = remoteProjectRecords.mapValues {
            Pair(it.value.first, it.value.first.getValues(values))
        }.toMutableMap()

        ErrorLogger.instance.log("RemoteSharedProjectManager.save values: $values")

        if (values.isNotEmpty()) {
            check(!isSaved)

            remoteProjectRecords = newRemoteProjectRecords

            databaseWrapper.updateRecords(values, getDatabaseCallback(extra))
        } else {
            saveCallback?.invoke()
        }

        return isSaved
    }

    fun newRemoteProjectRecord(jsonWrapper: JsonWrapper) = RemoteSharedProjectRecord(databaseWrapper, this, jsonWrapper).also {
        check(!remoteProjectRecords.containsKey(it.id))

        remoteProjectRecords[it.id] = Pair(it, false)
    }

    override fun deleteRemoteSharedProjectRecord(id: ProjectKey<ProjectType.Shared>) {
        remoteProjectRecords.remove(id)
    }
}
