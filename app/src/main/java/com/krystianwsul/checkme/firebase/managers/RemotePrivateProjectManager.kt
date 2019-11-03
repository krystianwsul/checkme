package com.krystianwsul.checkme.firebase.managers

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.records.RemotePrivateProjectRecord
import com.krystianwsul.common.time.ExactTimeStamp
import java.util.*
import kotlin.properties.Delegates

abstract class RemotePrivateProjectManager(
        private val domainFactory: DomainFactory,
        dataSnapshot: DataSnapshot,
        now: ExactTimeStamp
) {

    var isSaved by Delegates.observable(false) { _, _, value -> MyCrashlytics.log("RemotePrivateProjectManager.isSaved = $value") }

    var remoteProjectRecord = if (dataSnapshot.value == null) {
        RemotePrivateProjectRecord(domainFactory.deviceDbInfo, PrivateProjectJson(startTime = now.long))
    } else {
        dataSnapshot.toRecord()
    }
        private set

    private fun DataSnapshot.toRecord() = RemotePrivateProjectRecord(key!!, getValue(PrivateProjectJson::class.java)!!)

    fun newSnapshot(dataSnapshot: DataSnapshot): RemotePrivateProjectRecord {
        remoteProjectRecord = dataSnapshot.toRecord()
        return remoteProjectRecord
    }

    fun save(): Boolean {
        val values = HashMap<String, Any?>()

        remoteProjectRecord.getValues(values)

        MyCrashlytics.log("RemotePrivateProjectManager.save values: $values")

        if (values.isNotEmpty()) {
            check(!isSaved)

            isSaved = true
            AndroidDatabaseWrapper.updatePrivateProject(values).checkError(domainFactory, "RemotePrivateProjectManager.save", values)
        }

        return isSaved
    }
}
