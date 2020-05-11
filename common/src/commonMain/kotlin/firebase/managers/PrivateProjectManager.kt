package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.records.PrivateProjectRecord

abstract class PrivateProjectManager : ValueRecordManager<List<PrivateProjectRecord>>() {

    abstract val databaseWrapper: DatabaseWrapper

    abstract val privateProjectRecords: List<PrivateProjectRecord>

    override val records get() = privateProjectRecords

    override val value get() = privateProjectRecords

    override fun save(values: MutableMap<String, Any?>) {
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
