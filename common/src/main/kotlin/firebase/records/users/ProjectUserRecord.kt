package com.krystianwsul.common.firebase.records.users

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.UserData
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.firebase.records.RemoteRecord
import com.krystianwsul.common.firebase.records.project.SharedProjectRecord


class ProjectUserRecord(
        create: Boolean,
        private val remoteProjectRecord: SharedProjectRecord,
        override val createObject: UserJson
) : RemoteRecord(create) {

    companion object {

        private const val USERS = "users"
    }

    val id by lazy { UserData.getKey(createObject.email) }

    override val key by lazy { remoteProjectRecord.childKey + "/" + USERS + "/" + id.key }

    var name by Committer(createObject::name)

    val email by lazy { createObject.email }

    var photoUrl by Committer(createObject::photoUrl)

    fun setToken(deviceDbInfo: DeviceDbInfo) {
        check(deviceDbInfo.uuid.isNotEmpty())

        if (deviceDbInfo.deviceInfo.token == createObject.tokens[deviceDbInfo.uuid])
            return

        createObject.tokens[deviceDbInfo.uuid] = deviceDbInfo.deviceInfo.token
        addValue("$key/tokens/${deviceDbInfo.uuid}", deviceDbInfo.deviceInfo.token)
    }

    override fun deleteFromParent() = check(remoteProjectRecord.userRecords.remove(id) == this)
}
