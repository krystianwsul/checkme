package com.krystianwsul.checkme.firebase.managers

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.managers.RemotePrivateProjectManager
import com.krystianwsul.common.firebase.records.RemotePrivateProjectRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey

class AndroidRemotePrivateProjectManager(
        userInfo: UserInfo,
        dataSnapshot: DataSnapshot,
        now: ExactTimeStamp
) : RemotePrivateProjectManager<DomainFactory>() {

    var remoteProjectRecord = if (dataSnapshot.value == null) {
        RemotePrivateProjectRecord(AndroidDatabaseWrapper, userInfo, PrivateProjectJson(startTime = now.long))
    } else {
        dataSnapshot.toRecord()
    }
        private set

    override val remotePrivateProjectRecords get() = listOf(remoteProjectRecord)

    override val databaseWrapper = AndroidDatabaseWrapper

    private fun DataSnapshot.toRecord() = RemotePrivateProjectRecord(
            AndroidDatabaseWrapper,
            ProjectKey.Private(key!!),
            getValue(PrivateProjectJson::class.java)!!
    )

    fun newSnapshot(dataSnapshot: DataSnapshot): RemotePrivateProjectRecord {
        remoteProjectRecord = dataSnapshot.toRecord()
        return remoteProjectRecord
    }

    override fun getDatabaseCallback(extra: DomainFactory, values: Map<String, Any?>): DatabaseCallback {
        return checkError(extra, "RemotePrivateProjectManager.save", values)
    }
}