package com.krystianwsul.common.firebase.json

import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
@Parcelize
data class UserWrapper @JvmOverloads constructor(
        val userData: UserJson = UserJson(),
        val projects: MutableMap<String, Boolean> = mutableMapOf(),
        val friends: MutableMap<String, Boolean> = mutableMapOf(),
) : Parcelable