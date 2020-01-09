package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.UserData
import com.krystianwsul.common.firebase.json.UserJson


class RemoteProjectUserRecord(
        create: Boolean,
        private val remoteProjectRecord: RemoteSharedProjectRecord,
        override val createObject: UserJson
) : RemoteRecord(create) {

    companion object {

        private const val USERS = "users"
    }

    val id by lazy { UserData.getKey(createObject.email) }

    override val key by lazy { remoteProjectRecord.childKey + "/" + USERS + "/" + id }

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

    override fun deleteFromParent() = check(remoteProjectRecord.remoteUserRecords.remove(id) == this)
}
