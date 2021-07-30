package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.MyUserProperties
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.utils.UserKey


class MyUserRecord(
        databaseWrapper: DatabaseWrapper,
        create: Boolean,
        createObject: UserWrapper,
        userKey: UserKey,
) : RootUserRecord(databaseWrapper, create, createObject, userKey), MyUserProperties {

    override fun setToken(deviceDbInfo: DeviceDbInfo) {
        if (deviceDbInfo.token == userJson.tokens[deviceDbInfo.uuid])
            return

        userJson.tokens[deviceDbInfo.uuid] = deviceDbInfo.token

        addValue("$key/$USER_DATA/tokens/${deviceDbInfo.uuid}", deviceDbInfo.token)
        addValue("$key/$USER_DATA/uid", deviceDbInfo.userInfo.uid)
    }

    override var photoUrl by Committer(userJson::photoUrl, "$key/$USER_DATA")
}
