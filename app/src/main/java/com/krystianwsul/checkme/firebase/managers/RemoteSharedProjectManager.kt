package com.krystianwsul.checkme.firebase.managers

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.records.RemoteSharedProjectRecord
import java.util.*
import kotlin.properties.Delegates

class RemoteSharedProjectManager(
        private val domainFactory: DomainFactory,
        children: Iterable<DataSnapshot>
) : RemoteSharedProjectRecord.Parent {

    private fun DataSnapshot.toRecord() = RemoteSharedProjectRecord(this@RemoteSharedProjectManager, domainFactory.uuid, key!!, getValue(JsonWrapper::class.java)!!)

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

        remoteProjectRecords.values.filter { it.getValues(values) }

        MyCrashlytics.log("RemoteSharedProjectManager.save values: $values")

        if (values.isNotEmpty()) {
            check(!isSaved)

            isSaved = true
            AndroidDatabaseWrapper.updateRecords(values).checkError(domainFactory, "RemoteSharedProjectManager.save")
        }

        return isSaved
    }

    fun newRemoteProjectRecord(domainFactory: DomainFactory, jsonWrapper: JsonWrapper) = RemoteSharedProjectRecord(this, domainFactory.uuid, jsonWrapper).also {
        check(!remoteProjectRecords.containsKey(it.id))

        remoteProjectRecords[it.id] = it
    }

    override fun deleteRemoteSharedProjectRecord(id: String) {
        remoteProjectRecords.remove(id)
    }
}
