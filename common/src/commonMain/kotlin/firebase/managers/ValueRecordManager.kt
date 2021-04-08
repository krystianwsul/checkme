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

        check(newIsSaved == myValues.isNotEmpty())
        if (myValues.isNotEmpty()) {
            ErrorLogger.instance.log("${this::class.simpleName}.save values: $myValues")

            check(!isSaved)

            isSaved = newIsSaved

            values += myValues.mapKeys { "$databasePrefix/${it.key}" }
        }
    }

    protected fun set(throwIfUnequal: (T) -> Unit, valueCallback: () -> T): ChangeWrapper<T> { // lazy to prevent parsing if LOCAL
        return if (isSaved) { // todo isSaved propagate
            throwIfUnequal(value)

            isSaved = false

            ChangeWrapper(ChangeType.LOCAL, value) // todo issaved emit
        } else {
            value = valueCallback()

            ChangeWrapper(ChangeType.REMOTE, value)
        }
    }
}