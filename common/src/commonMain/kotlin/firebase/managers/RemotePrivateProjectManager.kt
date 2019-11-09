package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.records.RemotePrivateProjectRecord
import kotlin.properties.Delegates

abstract class RemotePrivateProjectManager {

    var isSaved by Delegates.observable(false) { _, _, value -> ErrorLogger.instance.log("RemotePrivateProjectManager.isSaved = $value") }

    abstract val databaseWrapper: DatabaseWrapper

    abstract val remotePrivateProjectRecords: List<RemotePrivateProjectRecord>

    protected abstract fun getDatabaseCallback(values: Any): DatabaseCallback

    open val saveCallback: (() -> Unit)? = null

    fun save(): Boolean {
        val values = HashMap<String, Any?>()

        remotePrivateProjectRecords.forEach { it.getValues(values) }

        ErrorLogger.instance.log("RemotePrivateProjectManager.save values: $values")

        if (values.isNotEmpty()) {
            check(!isSaved)

            isSaved = true
            databaseWrapper.updatePrivateProjects(values, getDatabaseCallback(values))
        } else {
            saveCallback?.invoke()
        }

        return isSaved
    }
}
