package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.records.RemoteRecord

abstract class KeyedRecordManager<T, U : RemoteRecord> : RecordManager {

    override val isSaved get() = recordPairs.any { it.value.second }

    protected open var recordPairs = mutableMapOf<T, Pair<U, Boolean>>()

    val records get() = recordPairs.mapValues { it.value.first }

    abstract val databasePrefix: String

    override fun save(values: MutableMap<String, Any?>) {
        val myValues = mutableMapOf<String, Any?>()

        val newRecords = recordPairs.mapValues {
            Pair(it.value.first, it.value.first.getValues(myValues))
        }.toMutableMap()

        ErrorLogger.instance.log("${this::class.simpleName}.save values: $myValues")

        if (myValues.isNotEmpty()) {
            check(!isSaved)

            recordPairs = newRecords

            values += myValues.mapKeys { "$databasePrefix/${it.key}" }
        }
    }

    fun remove(key: T) {
        checkNotNull(recordPairs.remove(key))
    }

    protected fun add(key: T, record: U) {
        check(!recordPairs.containsKey(key))

        recordPairs[key] = Pair(record, false)
    }

    protected fun setNullable(key: T, recordCallback: () -> U?): ChangeWrapper<U>? { // lazy to prevent parsing if LOCAL
        val pair = recordPairs[key]

        return if (pair?.second == true) {
            recordPairs[key] = Pair(pair.first, false)

            ChangeWrapper(ChangeType.LOCAL, pair.first)
        } else {
            val record = recordCallback() ?: return null

            recordPairs[key] = Pair(record, false)

            ChangeWrapper(ChangeType.REMOTE, record)
        }
    }

    protected fun setNonNull(key: T, recordCallback: () -> U) = setNullable(key, recordCallback)!!
}