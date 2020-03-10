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

    override val remoteRootUserRecords = children.map { RemoteRootUserRecord(false, it.getValue(UserWrapper::class.java)!!) }.associateBy { it.id }

    override val databaseWrapper = AndroidDatabaseWrapper

    override fun getDatabaseCallback(values: Map<String, Any?>): DatabaseCallback {
        return { databaseMessage, successful, exception ->
            val message = "firebase write: RemotePrivateProjectManager.save $databaseMessage, \nvalues: $values"
            if (successful) {
                MyCrashlytics.log(message)
            } else {
                MyCrashlytics.logException(FirebaseWriteException(message, exception))
            }
        }
    }
}
