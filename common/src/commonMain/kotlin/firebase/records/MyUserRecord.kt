package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.MyUserProperties
import com.krystianwsul.common.firebase.json.UserWrapper


class MyUserRecord(
        create: Boolean,
        createObject: UserWrapper
) : RootUserRecord(create, createObject), MyUserProperties {

    override fun setToken(deviceDbInfo: DeviceDbInfo) {
        if (deviceDbInfo.token == userJson.tokens[deviceDbInfo.uuid])
            return

        userJson.tokens[deviceDbInfo.uuid] = deviceDbInfo.token

        addValue("$key/$USER_DATA/tokens/${deviceDbInfo.uuid}", deviceDbInfo.token)
    }

    override var photoUrl by Committer(userJson::photoUrl, "$key/$USER_DATA")
    override var defaultReminder by Committer(createObject::defaultReminder)
    override var defaultTab by Committer(createObject::defaultTab)
}
