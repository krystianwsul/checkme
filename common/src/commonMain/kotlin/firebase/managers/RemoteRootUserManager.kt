package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey

abstract class RemoteRootUserManager {

    val isSaved get() = remoteRootUserRecords.values.any { it.second }

    abstract var remoteRootUserRecords: MutableMap<UserKey, Pair<RootUserRecord, Boolean>>

    fun save(values: MutableMap<String, Any?>) {
        val myValues = mutableMapOf<String, Any?>()

        val newRemoteRootUserRecords = remoteRootUserRecords.mapValues {
            Pair(it.value.first, it.value.first.getValues(myValues))
        }.toMutableMap()

        ErrorLogger.instance.log("RemoteFriendManager.save values: $myValues")

        if (myValues.isNotEmpty()) {
            check(!isSaved)

            remoteRootUserRecords = newRemoteRootUserRecords
        }

        values += myValues.mapKeys { "${DatabaseWrapper.USERS_KEY}/${it.key}" }
    }
}