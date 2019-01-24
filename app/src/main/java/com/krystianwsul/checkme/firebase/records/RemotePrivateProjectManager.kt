package com.krystianwsul.checkme.firebase.records

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.json.ProjectJson
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import java.util.*
import kotlin.properties.Delegates

class RemotePrivateProjectManager(
        private val domainFactory: DomainFactory,
        userInfo: UserInfo,
        dataSnapshot: DataSnapshot,
        now: ExactTimeStamp) {

    var isSaved by Delegates.observable(false) { _, _, value -> MyCrashlytics.log("RemotePrivateProjectManager.isSaved = $value") }

    var remoteProjectRecord = if (dataSnapshot.value == null) {
        RemotePrivateProjectRecord(domainFactory, userInfo, ProjectJson(startTime = now.long))
    } else {
        dataSnapshot.toRecord()
    }
        private set

    private fun DataSnapshot.toRecord() = RemotePrivateProjectRecord(domainFactory, key!!, getValue(ProjectJson::class.java)!!)

    fun newSnapshot(dataSnapshot: DataSnapshot): RemotePrivateProjectRecord {
        remoteProjectRecord = dataSnapshot.toRecord()
        return remoteProjectRecord
    }

    fun save(): Boolean {
        val values = HashMap<String, Any?>()

        remoteProjectRecord.getValues(values)

        MyCrashlytics.log("RemotePrivateProjectManager.save values: $values")

        if (!values.isEmpty()) {
            check(!isSaved)

            isSaved = true
            DatabaseWrapper.updatePrivateProject(values).addOnCompleteListener {
                MyCrashlytics.log("RemotePrivateProjectManager.save result isCanceled: " + it.isCanceled + ", isComplete: " + it.isComplete + ", isSuccessful: " + it.isSuccessful + ", exception: " + it.exception)
            }
        }

        return isSaved
    }
}
