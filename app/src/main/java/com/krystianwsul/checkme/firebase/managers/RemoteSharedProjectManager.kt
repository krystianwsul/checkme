package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.records.RemoteSharedProjectRecord
import kotlin.properties.Delegates

abstract class RemoteSharedProjectManager : RemoteSharedProjectRecord.Parent {

    var isSaved by Delegates.observable(false) { _, _, value -> MyCrashlytics.log("RemoteSharedProjectManager.isSaved = $value") }

    abstract val remoteProjectRecords: MutableMap<String, RemoteSharedProjectRecord>

    protected abstract fun getDatabaseCallback(): DatabaseCallback

    fun save(): Boolean {
        val values = HashMap<String, Any?>()

        remoteProjectRecords.values.filter { it.getValues(values) }

        MyCrashlytics.log("RemoteSharedProjectManager.save values: $values")

        if (values.isNotEmpty()) {
            check(!isSaved)

            isSaved = true
            AndroidDatabaseWrapper.updateRecords(values, getDatabaseCallback())
        }

        return isSaved
    }

    fun newRemoteProjectRecord(jsonWrapper: JsonWrapper) = RemoteSharedProjectRecord(this, jsonWrapper).also {
        check(!remoteProjectRecords.containsKey(it.id))

        remoteProjectRecords[it.id] = it
    }

    override fun deleteRemoteSharedProjectRecord(id: String) {
        remoteProjectRecords.remove(id)
    }
}
