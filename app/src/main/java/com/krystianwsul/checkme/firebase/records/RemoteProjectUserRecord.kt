package com.krystianwsul.checkme.firebase.records

import android.text.TextUtils
import com.krystianwsul.checkme.firebase.UserData
import com.krystianwsul.checkme.firebase.json.UserJson
import junit.framework.Assert

class RemoteProjectUserRecord(create: Boolean, private val remoteProjectRecord: RemoteProjectRecord, override val createObject: UserJson) : RemoteRecord(create) {

    companion object {

        private const val USERS = "users"
    }

    val id by lazy { UserData.getKey(createObject.email) }

    override val key by lazy { remoteProjectRecord.key + "/" + RemoteProjectRecord.PROJECT_JSON + "/" + USERS + "/" + id }

    var name: String
        get() = createObject.name
        set(name) {
            createObject.name = name
            addValue("$key/name", name)
        }

    val email by lazy { createObject.email }

    fun setToken(token: String?, uuid: String) {
        Assert.assertTrue(!TextUtils.isEmpty(uuid))

        createObject.tokens[uuid] = token
        addValue("$key/tokens/$uuid", token)
    }
}
