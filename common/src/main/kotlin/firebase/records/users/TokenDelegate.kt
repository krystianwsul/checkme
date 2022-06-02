package com.krystianwsul.common.firebase.records.users

import com.krystianwsul.common.VersionInfo
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.json.users.UserJson

class TokenDelegate(
    private val prefix: String,
    private val userJson: UserJson,
    private val addValue: (String, Any?) -> Unit,
) {

    fun setToken(deviceDbInfo: DeviceDbInfo, versionInfo: VersionInfo) {
        if (userJson.tokens[deviceDbInfo.uuid] != deviceDbInfo.token) {
            userJson.tokens[deviceDbInfo.uuid] = deviceDbInfo.token

            addValue("$prefix/tokens/${deviceDbInfo.uuid}", deviceDbInfo.token)
        }

        if (userJson.uid != deviceDbInfo.userInfo.uid) {
            userJson.uid = deviceDbInfo.userInfo.uid

            addValue("$prefix/uid", deviceDbInfo.userInfo.uid)
        }

        fun getDeviceData() = userJson.deviceDatas[deviceDbInfo.uuid]
        fun getOrPutDeviceData() = userJson.deviceDatas.getOrPut(deviceDbInfo.uuid) { UserJson.DeviceData() }

        val deviceDataPrefix = "$prefix/deviceDatas/${deviceDbInfo.uuid}"

        if (getDeviceData()?.token != deviceDbInfo.token) {
            getOrPutDeviceData().token = deviceDbInfo.token

            addValue("$deviceDataPrefix/token", deviceDbInfo.token)
        }

        if (getDeviceData()?.appVersion != versionInfo.appVersion) {
            getOrPutDeviceData().appVersion = versionInfo.appVersion

            addValue("$deviceDataPrefix/appVersion", versionInfo.appVersion)
        }

        if (getDeviceData()?.osVersion != versionInfo.osVersion) {
            getOrPutDeviceData().osVersion = versionInfo.osVersion

            addValue("$deviceDataPrefix/osVersion", versionInfo.osVersion)
        }
    }
}