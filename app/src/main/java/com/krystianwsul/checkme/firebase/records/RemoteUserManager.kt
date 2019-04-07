package com.krystianwsul.checkme.firebase.records

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.json.UserJson
import com.krystianwsul.checkme.firebase.json.UserWrapper
import com.krystianwsul.checkme.utils.checkError
import java.util.*
import kotlin.properties.Delegates

class RemoteUserManager(
        private val domainFactory: DomainFactory,
        private val userInfo: UserInfo,
        uuid: String,
        dataSnapshot: DataSnapshot) {

    var isSaved by Delegates.observable(false) { _, _, value -> MyCrashlytics.log("RemoteUserManager.isSaved = $value") }

    var remoteUserRecord = if (dataSnapshot.value == null) {
        val userWrapper = UserWrapper(mutableMapOf(), UserJson(userInfo.email, userInfo.name, mutableMapOf(uuid to userInfo.token)))
        RemoteRootUserRecord(true, userWrapper)
    } else {
        dataSnapshot.toRecord()
    }
        private set

    private fun DataSnapshot.toRecord() = RemoteRootUserRecord(false, getValue(UserWrapper::class.java)!!)

    fun newSnapshot(dataSnapshot: DataSnapshot): RemoteRootUserRecord {
        remoteUserRecord = dataSnapshot.toRecord()
        return remoteUserRecord
    }

    fun save(): Boolean {
        val values = HashMap<String, Any?>()

        remoteUserRecord.getValues(values)

        MyCrashlytics.log("RemoteUserManager.save values: $values")

        if (!values.isEmpty()) {
            check(!isSaved)

            isSaved = true
            DatabaseWrapper.updateUser(userInfo.key, values).checkError(domainFactory, "RemoteUserManager.save", values)
        }

        return isSaved
    }
}
