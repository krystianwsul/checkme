package com.krystianwsul.checkme.firebase.records

import android.text.TextUtils
import com.krystianwsul.checkme.firebase.UserData
import com.krystianwsul.common.firebase.UserJson


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

    fun setToken(token: String?, uuid: String) { // todo remove
        check(!TextUtils.isEmpty(uuid))

        if (token == createObject.tokens[uuid])
            return

        createObject.tokens[uuid] = token
        addValue("$key/tokens/$uuid", token)
    }

    override fun deleteFromParent() = check(remoteProjectRecord.remoteUserRecords.remove(id) == this)
}
