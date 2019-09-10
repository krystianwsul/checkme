package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.common.firebase.UserData
import com.krystianwsul.common.firebase.json.UserWrapper


open class RemoteRootUserRecord(create: Boolean, override val createObject: UserWrapper) : RemoteRecord(create) {

    companion object {

        const val USER_DATA = "userData"
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

    open val photoUrl get() = userJson.photoUrl

    fun removeFriendOf(friendId: String) {
        check(friendId.isNotEmpty())

        val friendOf = createObject.friendOf
        check(friendOf.containsKey(friendId))
        checkNotNull(friendOf[friendId])

        friendOf.remove(friendId)

        addValue("$key/$FRIEND_OF/$friendId", null)
    }

    override fun deleteFromParent() = throw UnsupportedOperationException()
}
