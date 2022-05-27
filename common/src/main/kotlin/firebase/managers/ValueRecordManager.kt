package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.records.RemoteRecord

abstract class ValueRecordManager<T : Any> : RecordManager {

    private var _value: T? = null
    val value get() = _value!!

    abstract val records: Collection<RemoteRecord>

    protected abstract val databasePrefix: String

    protected fun setInitialValue(value: T) {
        _value = value
    }

    final override fun save(values: MutableMap<String, Any?>) {
        val myValues = mutableMapOf<String, Any?>()

        val newIsSaved = records.map { it.getValues(myValues) }.any { it }
        check(newIsSaved == myValues.isNotEmpty())

        if (myValues.isNotEmpty()) {
            ErrorLogger.instance.log("${this::class.simpleName}.save values: $myValues")

            values += myValues.mapKeys { "$databasePrefix/${it.key}" }
        }
    }

    protected fun set(valueChanged: (T) -> Boolean, valueCallback: () -> T): T? { // lazy to prevent parsing if LOCAL
        return if (_value?.let(valueChanged) != false) {
            _value = valueCallback()

            value
        } else {
            null
        }
    }
}