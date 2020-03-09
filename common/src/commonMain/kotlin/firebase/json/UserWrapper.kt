package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
class UserWrapper @JvmOverloads constructor(
        val friendOf: MutableMap<String, Boolean> = mutableMapOf(),
        val userData: UserJson = UserJson(),
        var defaultReminder: Boolean = true,
        var defaultTab: Int = 0,
        val projects: MutableMap<String, Boolean> = mutableMapOf()
)
