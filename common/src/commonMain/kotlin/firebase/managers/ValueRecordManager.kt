package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.records.RemoteRecord

abstract class ValueRecordManager<T : Any> : RecordManager {

    final override var isSaved = false
        protected set

    abstract var value: T
        protected set

    abstract val records: Collection<RemoteRecord>

    protected abstract val databasePrefix: String

    final override fun save(values: MutableMap<String, Any?>) {
        val myValues = mutableMapOf<String, Any?>()

        val newIsSaved = records.map { it.getValues(myValues) }.any { it }

        ErrorLogger.instance.log("${this::class.simpleName}.save values: $myValues")

        check(newIsSaved == myValues.isNotEmpty())
        if (myValues.isNotEmpty()) {
            check(!isSaved)

            isSaved = newIsSaved

            values += myValues.mapKeys { "$databasePrefix/${it.key}" }
        }
    }

    protected fun set(valueCallback: () -> T): ChangeWrapper<T> { // lazy to prevent parsing if LOCAL
        return if (isSaved) {
            isSaved = false

            ChangeWrapper(ChangeType.LOCAL, value)
        } else {
            value = valueCallback()

            ChangeWrapper(ChangeType.REMOTE, value)
        }
    }
}