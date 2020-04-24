package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.UserData
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.records.MyUserRecord
import com.krystianwsul.common.utils.UserKey
import java.util.*
import kotlin.properties.Delegates.observable

class MyUserManager(
        deviceDbInfo: DeviceDbInfo,
        dataSnapshot: Snapshot
) {

    var isSaved by observable(false) { _, _, value -> MyCrashlytics.log("MyUserManager.isSaved = $value") }

    var userRecord = if (!dataSnapshot.exists()) {
        val userWrapper = UserWrapper(deviceDbInfo.run { UserJson(email, name, mutableMapOf(uuid to token)) })

        MyUserRecord(true, userWrapper, UserData.getKey(deviceDbInfo.email))
    } else {
        dataSnapshot.toRecord()
    }
        private set

    private fun Snapshot.toRecord() = MyUserRecord(false, getValue(UserWrapper::class.java)!!, UserKey(key))

    fun newSnapshot(dataSnapshot: Snapshot): MyUserRecord {
        userRecord = dataSnapshot.toRecord()
        return userRecord
    }

    fun save(values: MutableMap<String, Any?>) {
        val myValues = HashMap<String, Any?>()

        userRecord.getValues(myValues)

        MyCrashlytics.log("RemoteUserManager.save values: $myValues")

        if (myValues.isNotEmpty()) {
            check(!isSaved)

            isSaved = true

            values += myValues.mapKeys { "${DatabaseWrapper.USERS_KEY}/${it.key}" }
        }
    }
}
