package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.records.RemoteRecord

abstract class MapRecordManager<T, U : RemoteRecord> : RecordManager {

    protected open var _records = mutableMapOf<T, U>()

    val records: Map<T, U> get() = _records

    protected abstract val databasePrefix: String

    override fun save(values: MutableMap<String, Any?>) {
        val myValues = mutableMapOf<String, Any?>()

        records.forEach { it.value.getValues(myValues) }

        if (myValues.isNotEmpty()) {
            ErrorLogger.instance.log("${this::class.simpleName}.save values: $myValues")

            values += myValues.mapKeys { "$databasePrefix/${it.key}" }
        }
    }

    fun remove(key: T) {
        _records.remove(key)
    }

    protected fun add(key: T, record: U) {
        check(!_records.containsKey(key))

        _records[key] = record
    }

    protected fun set(key: T, valueChanged: (U) -> Boolean, recordCallback: () -> U?): ChangeWrapper<U>? { // lazy to prevent parsing if LOCAL
        return if (_records[key]?.let(valueChanged) != false) {
            val record = recordCallback() ?: return null

            _records[key] = record

            ChangeWrapper(ChangeType.REMOTE, record)
        } else {
            null
        }
    }
}