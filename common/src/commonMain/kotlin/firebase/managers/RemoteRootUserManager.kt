package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey

abstract class RemoteRootUserManager {

    var isSaved = false

    abstract val remoteRootUserRecords: Map<UserKey, RootUserRecord>

    abstract val databaseWrapper: DatabaseWrapper

    fun save(values: MutableMap<String, Any?>) {
        val myValues = mutableMapOf<String, Any?>()

        remoteRootUserRecords.values.forEach { it.getValues(myValues) }

        ErrorLogger.instance.log("RemoteFriendManager.save values: $myValues")

        if (myValues.isNotEmpty()) {
            check(!isSaved)

            isSaved = true
        }

        values += myValues.mapKeys { "${DatabaseWrapper.USERS_KEY}/${it.key}" }
    }
}