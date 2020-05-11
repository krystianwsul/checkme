package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.records.RemoteRecord

abstract class ValueRecordManager<T : Any> : RecordManager {

    final override var isSaved = false
        protected set

    abstract val value: T

    abstract val records: Collection<RemoteRecord>

    abstract val databasePrefix: String

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
}