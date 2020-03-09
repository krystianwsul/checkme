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
import com.krystianwsul.common.utils.ProjectKey

class AndroidRemoteSharedProjectManager(children: Iterable<DataSnapshot>) : RemoteSharedProjectManager<DomainFactory>() {

    private fun DataSnapshot.toRecord() = RemoteSharedProjectRecord(
            AndroidDatabaseWrapper,
            this@AndroidRemoteSharedProjectManager,
            ProjectKey.Shared(key!!),
            getValue(JsonWrapper::class.java)!!
    )

    override var remoteProjectRecords = children.mapNotNull {
        try {
            ProjectKey.Shared(it.key!!) to Pair(it.toRecord(), false)
        } catch (onlyVisibilityPresentException: RemoteTaskRecord.OnlyVisibilityPresentException) {
            MyCrashlytics.logException(onlyVisibilityPresentException)

            null
        }
    }
            .toMap()
            .toMutableMap()

    override val databaseWrapper = AndroidDatabaseWrapper

    fun setChild(dataSnapshot: DataSnapshot): RemoteSharedProjectRecord {
        val key = ProjectKey.Shared(dataSnapshot.key!!)

        return dataSnapshot.toRecord().also {
            remoteProjectRecords[key] = Pair(it, false)
        }
    }

    override fun getDatabaseCallback(extra: DomainFactory) = checkError(extra, "RemoteSharedProjectManager.save")

    fun removeChild(key: ProjectKey.Shared) {
        check(remoteProjectRecords.remove(key) != null)
    }
}