package com.krystianwsul.common.firebase.records.users

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.json.users.UserJson

class TokenDelegate(
    private val prefix: String,
    private val userJson: UserJson,
    private val addValue: (String, Any?) -> Unit,
) {

    fun setToken(deviceDbInfo: DeviceDbInfo) {
        if (userJson.tokens[deviceDbInfo.uuid] != deviceDbInfo.token) {
            userJson.tokens[deviceDbInfo.uuid] = deviceDbInfo.token

            addValue("$prefix/tokens/${deviceDbInfo.uuid}", deviceDbInfo.token)
        }

        if (userJson.uid != deviceDbInfo.userInfo.uid) {
            userJson.uid = deviceDbInfo.userInfo.uid

            addValue("$prefix/uid", deviceDbInfo.userInfo.uid)
        }
    }
}