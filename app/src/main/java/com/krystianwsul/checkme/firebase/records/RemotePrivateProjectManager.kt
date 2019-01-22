package com.krystianwsul.checkme.firebase.records

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.json.ProjectJson
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import java.util.*

class RemotePrivateProjectManager(
        private val domainFactory: DomainFactory,
        userInfo: UserInfo,
        dataSnapshot: DataSnapshot,
        now: ExactTimeStamp) {

    var isSaved = false

    var remoteProjectRecord = if (dataSnapshot.value == null) {
        RemotePrivateProjectRecord(domainFactory, userInfo, ProjectJson(startTime = now.long))
    } else {
        dataSnapshot.toRecord()
    }
        private set

    private fun DataSnapshot.toRecord() = RemotePrivateProjectRecord(domainFactory, key!!, getValue(ProjectJson::class.java)!!)

    fun newSnapshot(dataSnapshot: DataSnapshot): RemotePrivateProjectRecord {
        remoteProjectRecord = dataSnapshot.toRecord()
        return remoteProjectRecord
    }

    fun save(): Boolean {
        val values = HashMap<String, Any?>()

        remoteProjectRecord.getValues(values)

        Log.e("asdf", "RemotePrivateProjectManager.save values: $values")

        if (!values.isEmpty()) {
            check(!isSaved)

            Log.e("asdf", "saving private project record:\n$values")

            isSaved = true
            DatabaseWrapper.updatePrivateProject(values)
        }

        return isSaved
    }
}
