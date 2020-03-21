package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.records.RemotePrivateProjectRecord
import kotlin.properties.Delegates

abstract class PrivateProjectManager<T> {

    var isSaved by Delegates.observable(false) { _, _, value -> ErrorLogger.instance.log("RemotePrivateProjectManager.isSaved = $value") }

    abstract val databaseWrapper: DatabaseWrapper

    abstract val privateProjectRecords: List<RemotePrivateProjectRecord>

    protected abstract fun getDatabaseCallback(extra: T, values: Map<String, Any?>): DatabaseCallback

    open val saveCallback: (() -> Unit)? = null

    fun save(extra: T): Boolean {
        val values = mutableMapOf<String, Any?>()

        privateProjectRecords.forEach { it.getValues(values) }

        ErrorLogger.instance.log("RemotePrivateProjectManager.save values: $values")

        if (values.isNotEmpty()) {
            check(!isSaved)

            isSaved = true
            databaseWrapper.updatePrivateProjects(values, getDatabaseCallback(extra, values))
        } else {
            saveCallback?.invoke()
        }

        return isSaved
    }
}
