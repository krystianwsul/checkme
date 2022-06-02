package com.krystianwsul.common.firebase.json.users

import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class UserJson @JvmOverloads constructor(
    val email: String = "",
    var name: String = "",
    val tokens: MutableMap<String, String?> = mutableMapOf(),
    var photoUrl: String? = null,
    var uid: String? = null,
    val deviceDatas: MutableMap<String, DeviceData> = mutableMapOf(), // added 2022-06-02
) : Parcelable {

    @Serializable
    @Parcelize
    data class DeviceData @JvmOverloads constructor(
        var token: String? = null,
        var appVersion: Int = 0,
        var osVersion: Int = 0,
    ) : Parcelable
}