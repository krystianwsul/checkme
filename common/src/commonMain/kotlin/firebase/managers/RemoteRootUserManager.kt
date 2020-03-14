package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.records.RemoteRootUserRecord
import com.krystianwsul.common.utils.UserKey

abstract class RemoteRootUserManager {

    var isSaved = false

    abstract val remoteRootUserRecords: Map<UserKey, RemoteRootUserRecord>

    abstract val databaseWrapper: DatabaseWrapper

    protected abstract fun getDatabaseCallback(): DatabaseCallback

    open val saveCallback: (() -> Unit)? = null

    fun save(): Boolean {
        val values = mutableMapOf<String, Any?>()

        remoteRootUserRecords.values.forEach { it.getValues(values) }

        if (values.isNotEmpty()) {
            check(!isSaved)

            isSaved = true
        }

        ErrorLogger.instance.log("RemoteFriendManager.save values: $values")

        if (values.isNotEmpty()) {
            databaseWrapper.updateFriends(values, getDatabaseCallback())
        } else {
            saveCallback?.invoke()
        }

        return isSaved
    }
}