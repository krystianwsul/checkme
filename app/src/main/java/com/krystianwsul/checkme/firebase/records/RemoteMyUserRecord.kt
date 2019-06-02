package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.firebase.RemoteMyUserInterface
import com.krystianwsul.checkme.firebase.json.UserWrapper


class RemoteMyUserRecord(
        create: Boolean,
        createObject: UserWrapper,
        private val uuid: String) : RemoteRootUserRecord(create, createObject), RemoteMyUserInterface {

    override fun setToken(token: String?) {
        if (token == userJson.tokens[uuid])
            return

        userJson.tokens[uuid] = token

        addValue("$key/$USER_DATA/tokens/$uuid", token)
    }

    override var photoUrl
        get() = super.photoUrl
        set(value) {
            if (value == userJson.photoUrl)
                return

            check(!value.isNullOrEmpty())

            userJson.photoUrl = value
            addValue("$key/$USER_DATA/photoUrl", value)
        }

    override var defaultReminder: Boolean
        get() = createObject.defaultReminder
        set(value) {
            if (value == createObject.defaultReminder)
                return

            createObject.defaultReminder = value
            addValue("$key/defaultReminder", value)
        }
}
