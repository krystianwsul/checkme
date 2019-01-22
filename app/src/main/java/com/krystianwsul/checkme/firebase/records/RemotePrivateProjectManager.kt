package com.krystianwsul.checkme.firebase.records

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.json.ProjectJson
import java.util.*

class RemotePrivateProjectManager(private val domainFactory: DomainFactory, dataSnapshot: DataSnapshot) {

    private fun DataSnapshot.toRecord() = RemotePrivateProjectRecord(domainFactory, key!!, getValue(ProjectJson::class.java)!!)

    var isSaved = false

    val key = dataSnapshot.key!!
    val remoteProjectRecord = dataSnapshot.toRecord()

    fun save(): Boolean {
        val values = HashMap<String, Any?>()

        remoteProjectRecord.getValues(values)

        Log.e("asdf", "RemotePrivateProjectManager.save values: $values")

        if (!values.isEmpty()) {
            check(!isSaved)

            Log.e("asdf", "saving private project record:\n$values")

            isSaved = true
            DatabaseWrapper.updatePrivateProject(key, values)
        }

        return isSaved
    }
}
