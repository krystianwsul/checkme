package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.RemoteMyUserInterface
import com.krystianwsul.common.firebase.json.UserWrapper


class MyUserRecord(
        create: Boolean,
        createObject: UserWrapper
) : RootUserRecord(create, createObject), RemoteMyUserInterface {

    override fun setToken(uuid: String, token: String?) {
        if (token == userJson.tokens[uuid])
            return

        userJson.tokens[uuid] = token

        addValue("$key/$USER_DATA/tokens/$uuid", token)
    }

    override var photoUrl by Committer(userJson::photoUrl, "$key/$USER_DATA")
    override var defaultReminder by Committer(createObject::defaultReminder)
    override var defaultTab by Committer(createObject::defaultTab)
}
