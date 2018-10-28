package com.krystianwsul.checkme.firebase.records

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.json.JsonWrapper
import java.util.*

class RemoteProjectManager(kotlinDomainFactory: KotlinDomainFactory, children: Iterable<DataSnapshot>) {

    var isSaved = false
        private set

    val remoteProjectRecords = mutableMapOf<String, RemoteProjectRecord>()

    init {
        for (child in children) {
            child.key.let {
                check(it.isNotEmpty())

                remoteProjectRecords[it] = RemoteProjectRecord(kotlinDomainFactory, it, child.getValue(JsonWrapper::class.java)!!)
            }
        }
    }

    fun save() {
        val values = HashMap<String, Any?>()

        remoteProjectRecords.values.forEach { it.getValues(values) }

        Log.e("asdf", "RemoteProjectManager.save values: $values")

        if (!values.isEmpty()) {
            isSaved = true
            DatabaseWrapper.updateRecords(values)
        }
    }

    fun newRemoteProjectRecord(kotlinDomainFactory: KotlinDomainFactory, jsonWrapper: JsonWrapper) = RemoteProjectRecord(kotlinDomainFactory, jsonWrapper).also {
        check(!remoteProjectRecords.containsKey(it.id))

        remoteProjectRecords[it.id] = it
    }
}
