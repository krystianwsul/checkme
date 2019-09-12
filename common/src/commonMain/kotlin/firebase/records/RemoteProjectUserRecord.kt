package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.UserData
import com.krystianwsul.common.firebase.json.UserJson


class RemoteProjectUserRecord(
        create: Boolean,
        private val remoteProjectRecord: RemoteSharedProjectRecord,
        override val createObject: UserJson) : RemoteRecord(create) {

    companion object {

        private const val USERS = "users"
    }

    val id by lazy { UserData.getKey(createObject.email) }

    override val key by lazy { remoteProjectRecord.childKey + "/" + USERS + "/" + id }

    var name: String
        get() = createObject.name
        set(name) {
            if (name == createObject.name)
                return

            createObject.name = name
            addValue("$key/name", name)
        }

    val email by lazy { createObject.email }

    var photoUrl: String?
        get() = createObject.photoUrl
        set(value) {
            if (value == createObject.photoUrl)
                return

            check(!value.isNullOrEmpty())

            createObject.photoUrl = value
            addValue("$key/photoUrl", value)
        }

    fun setToken(deviceDbInfo: DeviceDbInfo) {
        check(deviceDbInfo.uuid.isNotEmpty())

        if (deviceDbInfo.deviceInfo.token == createObject.tokens[deviceDbInfo.uuid])
            return

        createObject.tokens[deviceDbInfo.uuid] = deviceDbInfo.deviceInfo.token
        addValue("$key/tokens/$deviceDbInfo.uuid", deviceDbInfo.deviceInfo.token)
    }

    override fun deleteFromParent() = check(remoteProjectRecord.remoteUserRecords.remove(id) == this)
}
