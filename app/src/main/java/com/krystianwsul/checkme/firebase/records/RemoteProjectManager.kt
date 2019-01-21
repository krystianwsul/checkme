package com.krystianwsul.checkme.firebase.records

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.json.JsonWrapper
import java.util.*

class RemoteProjectManager(domainFactory: DomainFactory, children: Iterable<DataSnapshot>) {

    var isSaved = false

    val remoteProjectRecords = children.associate { child ->
        val key = child.key!!
        key to RemoteProjectRecord(domainFactory, key, child.getValue(JsonWrapper::class.java)!!)
    }.toMutableMap()

    fun save(): Boolean {
        val values = HashMap<String, Any?>()

        remoteProjectRecords.values.forEach { it.getValues(values) }

        Log.e("asdf", "RemoteProjectManager.save values: $values")

        if (!values.isEmpty()) {
            isSaved = true
            DatabaseWrapper.updateRecords(values)
        }

        return isSaved
    }

    fun newRemoteProjectRecord(domainFactory: DomainFactory, jsonWrapper: JsonWrapper) = RemoteProjectRecord(domainFactory, jsonWrapper).also {
        check(!remoteProjectRecords.containsKey(it.id))

        remoteProjectRecords[it.id] = it
    }
}
