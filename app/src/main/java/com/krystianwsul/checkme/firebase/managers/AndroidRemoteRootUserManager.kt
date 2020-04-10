package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.firebase.FirebaseWriteException
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.managers.RemoteRootUserManager
import com.krystianwsul.common.firebase.records.RootUserRecord

class AndroidRemoteRootUserManager(
        children: Iterable<Snapshot>,
        override val databaseWrapper: DatabaseWrapper
) : RemoteRootUserManager() {

    companion object {

        private fun Iterable<Snapshot>.toRootUserRecords() = map {
            RootUserRecord(false, it.getValue(UserWrapper::class.java)!!)
        }.associateBy { it.id }
    }

    override var remoteRootUserRecords = children.toRootUserRecords()

    override fun getDatabaseCallback(): DatabaseCallback {
        return { databaseMessage, successful, exception ->
            val message = "firebase write: RemotePrivateProjectManager.save $databaseMessage"
            if (successful) {
                MyCrashlytics.log(message)
            } else {
                MyCrashlytics.logException(FirebaseWriteException(message, exception))
            }
        }
    }

    fun onNewSnapshot(children: Iterable<Snapshot>) = children.toRootUserRecords().also {
        remoteRootUserRecords = it
    }
}
