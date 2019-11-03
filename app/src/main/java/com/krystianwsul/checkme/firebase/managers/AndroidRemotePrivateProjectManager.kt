package com.krystianwsul.checkme.firebase.managers

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.records.RemotePrivateProjectRecord
import com.krystianwsul.common.time.ExactTimeStamp

class AndroidRemotePrivateProjectManager(
        private val domainFactory: DomainFactory,
        dataSnapshot: DataSnapshot,
        now: ExactTimeStamp
) : RemotePrivateProjectManager() {

    var remoteProjectRecord = if (dataSnapshot.value == null) {
        RemotePrivateProjectRecord(domainFactory.deviceDbInfo, PrivateProjectJson(startTime = now.long))
    } else {
        dataSnapshot.toRecord()
    }
        private set

    override val remotePrivateProjectRecords get() = listOf(remoteProjectRecord)

    private fun DataSnapshot.toRecord() = RemotePrivateProjectRecord(key!!, getValue(PrivateProjectJson::class.java)!!)

    fun newSnapshot(dataSnapshot: DataSnapshot): RemotePrivateProjectRecord {
        remoteProjectRecord = dataSnapshot.toRecord()
        return remoteProjectRecord
    }

    override fun getDatabaseCallback(values: Any): DatabaseCallback {
        return checkError(domainFactory, "RemotePrivateProjectManager.save", values)
    }
}