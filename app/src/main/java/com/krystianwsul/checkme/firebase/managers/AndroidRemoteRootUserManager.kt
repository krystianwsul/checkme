package com.krystianwsul.checkme.firebase.managers

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.firebase.FirebaseWriteException
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.managers.RemoteRootUserManager
import com.krystianwsul.common.firebase.records.RemoteRootUserRecord

class AndroidRemoteRootUserManager(children: Iterable<DataSnapshot>) : RemoteRootUserManager() {

    companion object {

        private fun Iterable<DataSnapshot>.toRootUserRecords() = map {
            RemoteRootUserRecord(false, it.getValue(UserWrapper::class.java)!!)
        }.associateBy { it.id }
    }

    override var remoteRootUserRecords = children.toRootUserRecords()

    override val databaseWrapper = AndroidDatabaseWrapper

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

    fun onNewSnapshot(children: Iterable<DataSnapshot>) = children.toRootUserRecords().also {
        remoteRootUserRecords = it
    }
}
