package com.krystianwsul.checkme.firebase.records

import android.text.TextUtils
import com.krystianwsul.checkme.firebase.UserData
import com.krystianwsul.checkme.firebase.json.UserWrapper


class RemoteRootUserRecord(create: Boolean, override val createObject: UserWrapper) : RemoteRecord(create) {

    companion object {

        private const val USER_DATA = "userData"
        private const val FRIEND_OF = "friendOf"
    }

    val userJson by lazy { createObject.userData }

    val id by lazy { UserData.getKey(userJson.email) }

    override val key by lazy { id }

    var name: String
        get() = userJson.name
        set(name) {
            if (name == userJson.name)
                return

            userJson.name = name
            addValue("$key/$USER_DATA/name", name)
        }

    val email by lazy { userJson.email }

    fun removeFriendOf(friendId: String) {
        check(!TextUtils.isEmpty(friendId))

        val friendOf = createObject.friendOf
        check(friendOf.containsKey(friendId))
        checkNotNull(friendOf[friendId])

        friendOf.remove(friendId)

        addValue("$key/$FRIEND_OF/$friendId", null)
    }
}
