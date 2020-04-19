package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import kotlin.properties.Delegates

abstract class PrivateProjectManager<T> {

    var isSaved by Delegates.observable(false) { _, _, value ->
        ErrorLogger.instance.log("PrivateProjectManager.isSaved = $value")
    }
        protected set

    abstract val databaseWrapper: DatabaseWrapper

    abstract val privateProjectRecords: List<PrivateProjectRecord>

    fun save(values: MutableMap<String, Any?>) {
        val myValues = mutableMapOf<String, Any?>()

        privateProjectRecords.forEach { it.getValues(myValues) }

        ErrorLogger.instance.log("RemotePrivateProjectManager.save values: $myValues")

        if (myValues.isNotEmpty()) {
            check(!isSaved)

            isSaved = true

            values += myValues.mapKeys { "${DatabaseWrapper.PRIVATE_PROJECTS_KEY}/${it.key}" }
        }
    }
}
