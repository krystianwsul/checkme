package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.FactoryProvider
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.records.MyUserRecord
import java.util.*
import kotlin.properties.Delegates.observable

class RemoteMyUserManager(
        deviceInfo: DeviceInfo,
        private val uuid: String,
        dataSnapshot: FactoryProvider.Database.Snapshot
) {

    var isSaved by observable(false) { _, _, value -> MyCrashlytics.log("RemoteUserManager.isSaved = $value") }

    var remoteUserRecord = if (!dataSnapshot.exists()) {
        val userWrapper = UserWrapper(
                mutableMapOf(),
                deviceInfo.run { UserJson(email, name, mutableMapOf(uuid to token)) }
        )

        MyUserRecord(true, userWrapper)
    } else {
        dataSnapshot.toRecord()
    }
        private set

    private fun FactoryProvider.Database.Snapshot.toRecord() = MyUserRecord(false, getValue(UserWrapper::class.java)!!)

    fun newSnapshot(dataSnapshot: FactoryProvider.Database.Snapshot): MyUserRecord {
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
