package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.records.RemoteRecord

abstract class KeyedRecordManager<T, U : RemoteRecord> : RecordManager {

    override val isSaved get() = records.any { it.value.second }

    var records = mutableMapOf<T, Pair<U, Boolean>>()
        private set

    abstract val databaseWrapper: DatabaseWrapper

    abstract val databasePrefix: String

    override fun save(values: MutableMap<String, Any?>) {
        val myValues = mutableMapOf<String, Any?>()

        val newRecords = records.mapValues {
            Pair(it.value.first, it.value.first.getValues(myValues))
        }.toMutableMap()

        ErrorLogger.instance.log("${this::class.simpleName}.save values: $myValues")

        if (myValues.isNotEmpty()) {
            check(!isSaved)

            records = newRecords

            values += myValues.mapKeys { "$databasePrefix/${it.key}" }
        }
    }
}