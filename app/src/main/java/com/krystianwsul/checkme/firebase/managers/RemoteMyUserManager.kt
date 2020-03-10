package com.krystianwsul.checkme.firebase.managers

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.records.RemoteMyUserRecord
import java.util.*
import kotlin.properties.Delegates.observable

class RemoteMyUserManager(
        deviceInfo: DeviceInfo,
        private val uuid: String,
        dataSnapshot: DataSnapshot
) {

    var isSaved by observable(false) { _, _, value -> MyCrashlytics.log("RemoteUserManager.isSaved = $value") }

    var remoteUserRecord = if (dataSnapshot.value == null) {
        val userWrapper = UserWrapper(
                mutableMapOf(),
                deviceInfo.run { UserJson(email, name, mutableMapOf(uuid to token)) }
        )

        RemoteMyUserRecord(true, userWrapper)
    } else {
        dataSnapshot.toRecord()
    }
        private set

    private fun DataSnapshot.toRecord() = RemoteMyUserRecord(false, getValue(UserWrapper::class.java)!!)

    fun newSnapshot(dataSnapshot: DataSnapshot): RemoteMyUserRecord {
        remoteUserRecord = dataSnapshot.toRecord()
        return remoteUserRecord
    }

    fun save(domainFactory: DomainFactory): Boolean {
        val values = HashMap<String, Any?>()

        remoteUserRecord.getValues(values)

        MyCrashlytics.log("RemoteUserManager.save values: $values")

        if (values.isNotEmpty()) {
            check(!isSaved)

            isSaved = true
            AndroidDatabaseWrapper.updateFriends(values).checkError(domainFactory, "RemoteUserManager.save", values)
        }

        return isSaved
    }
}
