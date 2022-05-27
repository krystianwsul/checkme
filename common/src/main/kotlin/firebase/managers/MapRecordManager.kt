package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.records.RemoteRecord
import com.krystianwsul.common.utils.NullableWrapper

abstract class MapRecordManager<T, U : Any> : RecordManager {

    private var recordMap = mutableMapOf<T, U>()

    val records: Map<T, U> get() = recordMap

    protected abstract val databasePrefix: String

    protected fun setInitialRecords(records: Map<T, U>) {
        recordMap = records.toMutableMap()
    }

    protected abstract fun valueToRecord(value: U): RemoteRecord

    override fun save(values: MutableMap<String, Any?>) {
        val myValues = mutableMapOf<String, Any?>()

        records.forEach { valueToRecord(it.value).getValues(myValues) }

        if (myValues.isNotEmpty()) {
            ErrorLogger.instance.log("${this::class.simpleName}.save values: $myValues")

            values += myValues.mapKeys { "$databasePrefix/${it.key}" }
        }
    }

    fun remove(key: T) {
        recordMap.remove(key)
    }

    protected fun add(key: T, record: U) {
        check(!recordMap.containsKey(key))

        recordMap[key] = record
    }

    protected fun set(
        key: T,
        valueChanged: (U) -> Boolean,
        recordCallback: () -> U?
    ): ChangeWrapper<U>? { // lazy to prevent parsing if LOCAL
        return setNullable(key, valueChanged, recordCallback)?.let { changeWrapper ->
            changeWrapper.value?.let { ChangeWrapper(ChangeType.REMOTE, it) } // todo changeType
        }
    }

    protected fun setNullable(
        key: T,
        valueChanged: (U) -> Boolean,
        recordCallback: () -> U?
    ): NullableWrapper<U>? { // lazy to prevent parsing if LOCAL
        return if (recordMap[key]?.let(valueChanged) != false) {
            val record = recordCallback() ?: return NullableWrapper()

            recordMap[key] = record

            NullableWrapper(record)
        } else {
            null
        }
    }
}