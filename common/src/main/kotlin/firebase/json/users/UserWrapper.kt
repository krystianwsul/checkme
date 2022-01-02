package com.krystianwsul.common.firebase.json.users

import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.firebase.json.customtimes.UserCustomTimeJson
import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class UserWrapper @JvmOverloads constructor(
    val userData: UserJson = UserJson(),
    val projects: MutableMap<String, Boolean> = mutableMapOf(),
    val friends: MutableMap<String, Boolean> = mutableMapOf(),
    var customTimes: MutableMap<String, UserCustomTimeJson> = mutableMapOf(),
) : Parcelable, Parsable