package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey

abstract class RootUserManager : RecordManager {

    override val isSaved get() = rootUserRecords.values.any { it.second }

    abstract var rootUserRecords: MutableMap<UserKey, Pair<RootUserRecord, Boolean>>

    override fun save(values: MutableMap<String, Any?>) {
        val myValues = mutableMapOf<String, Any?>()

        val newRootUserRecords = rootUserRecords.mapValues {
            Pair(it.value.first, it.value.first.getValues(myValues))
        }.toMutableMap()

        ErrorLogger.instance.log("RootUserManager.save values: $myValues")

        if (myValues.isNotEmpty()) {
            check(!isSaved)

            rootUserRecords = newRootUserRecords
        }

        values += myValues.mapKeys { "${DatabaseWrapper.USERS_KEY}/${it.key}" }
    }
}