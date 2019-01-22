package com.krystianwsul.checkme.firebase.records

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.json.JsonWrapper
import java.util.*

class RemoteProjectManager(private val domainFactory: DomainFactory, children: Iterable<DataSnapshot>) {

    private fun DataSnapshot.toRecord() = RemoteProjectRecord(domainFactory, key!!, getValue(JsonWrapper::class.java)!!)

    var isSaved = false

    val remoteProjectRecords = children.associate { child -> child.key!! to child.toRecord() }.toMutableMap()

    fun addChild(dataSnapshot: DataSnapshot): RemoteProjectRecord {
        val key = dataSnapshot.key!!
        check(!remoteProjectRecords.containsKey(key))

        return dataSnapshot.toRecord().also {
            remoteProjectRecords[key] = it
        }
    }

    fun changeChild(dataSnapshot: DataSnapshot): RemoteProjectRecord {
        val key = dataSnapshot.key!!
        check(remoteProjectRecords.containsKey(key))

        return dataSnapshot.toRecord().also {
            remoteProjectRecords[key] = it
        }
    }

    fun removeChild(dataSnapshot: DataSnapshot) = dataSnapshot.key!!.also {
        check(remoteProjectRecords.remove(it) != null)
    }

    fun save(): Boolean {
        val values = HashMap<String, Any?>()

        remoteProjectRecords.values.forEach { it.getValues(values) }

        Log.e("asdf", "RemoteProjectManager.save values: $values")

        if (!values.isEmpty()) {
            check(!isSaved)

            Log.e("asdf", "saving task records:\n$values")

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
