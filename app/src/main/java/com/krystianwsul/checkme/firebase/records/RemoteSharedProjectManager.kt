package com.krystianwsul.checkme.firebase.records

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.json.JsonWrapper
import com.krystianwsul.checkme.utils.checkError
import java.util.*
import kotlin.properties.Delegates

class RemoteSharedProjectManager(private val domainFactory: DomainFactory, children: Iterable<DataSnapshot>) {

    private fun DataSnapshot.toRecord() = RemoteSharedProjectRecord(domainFactory, key!!, getValue(JsonWrapper::class.java)!!)

    var isSaved by Delegates.observable(false) { _, _, value -> MyCrashlytics.log("RemoteSharedProjectManager.isSaved = $value") }

    val remoteProjectRecords = children.associate { child -> child.key!! to child.toRecord() }.toMutableMap()

    fun addChild(dataSnapshot: DataSnapshot): RemoteSharedProjectRecord {
        val key = dataSnapshot.key!!
        check(!remoteProjectRecords.containsKey(key))

        return dataSnapshot.toRecord().also {
            remoteProjectRecords[key] = it
        }
    }

    fun changeChild(dataSnapshot: DataSnapshot): RemoteSharedProjectRecord {
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

        MyCrashlytics.log("RemoteSharedProjectManager.save values: $values")

        if (!values.isEmpty()) {
            check(!isSaved)

            isSaved = true
            DatabaseWrapper.updateRecords(values).checkError("RemoteSharedProjectManager.save")
        }

        return isSaved
    }

    fun newRemoteProjectRecord(domainFactory: DomainFactory, jsonWrapper: JsonWrapper) = RemoteSharedProjectRecord(domainFactory, jsonWrapper).also {
        check(!remoteProjectRecords.containsKey(it.id))

        remoteProjectRecords[it.id] = it
    }
}
