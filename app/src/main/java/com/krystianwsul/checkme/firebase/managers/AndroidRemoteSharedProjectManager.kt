package com.krystianwsul.checkme.firebase.managers

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.managers.RemoteSharedProjectManager
import com.krystianwsul.common.firebase.records.RemoteSharedProjectRecord
import com.krystianwsul.common.firebase.records.RemoteTaskRecord

class AndroidRemoteSharedProjectManager(
        private val domainFactory: DomainFactory,
        children: Iterable<DataSnapshot>
) : RemoteSharedProjectManager() {

    private fun DataSnapshot.toRecord() = RemoteSharedProjectRecord(AndroidDatabaseWrapper, this@AndroidRemoteSharedProjectManager, key!!, getValue(JsonWrapper::class.java)!!)

    override val remoteProjectRecords = children.mapNotNull {
        try {
            it.key!! to it.toRecord()
        } catch (onlyVisibilityPresentException: RemoteTaskRecord.OnlyVisibilityPresentException) {
            MyCrashlytics.logException(onlyVisibilityPresentException)

            null
        }
    }
            .toMap()
            .toMutableMap()

    override val databaseWrapper = AndroidDatabaseWrapper

    fun setChild(dataSnapshot: DataSnapshot): RemoteSharedProjectRecord {
        val key = dataSnapshot.key!!

        return dataSnapshot.toRecord().also {
            remoteProjectRecords[key] = it
        }
    }

    override fun getDatabaseCallback() = checkError(domainFactory, "RemoteSharedProjectManager.save")

    fun removeChild(dataSnapshot: DataSnapshot) = dataSnapshot.key!!.also {
        check(remoteProjectRecords.remove(it) != null)
    }
}