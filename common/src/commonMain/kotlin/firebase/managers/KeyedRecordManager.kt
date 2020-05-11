package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.records.RemoteRecord

abstract class KeyedRecordManager<T, U : RemoteRecord> : RecordManager {

    override val isSaved get() = records.any { it.value.second }

    open var records = mutableMapOf<T, Pair<U, Boolean>>()
        protected set

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

    fun remove(key: T) {
        checkNotNull(records.remove(key))
    }

    fun add(key: T, record: U) {
        check(!records.containsKey(key))

        records[key] = Pair(record, false)
    }

    fun set(key: T, recordCallback: () -> U?): ChangeWrapper<U>? { // lazy to prevent parsing if LOCAL
        val pair = records[key]

        return if (pair?.second == true) {
            records[key] = Pair(pair.first, false)

            ChangeWrapper(ChangeType.LOCAL, pair.first)
        } else {
            val record = recordCallback() ?: return null

            records[key] = Pair(record, false)

            ChangeWrapper(ChangeType.REMOTE, record)
        }
    }
}