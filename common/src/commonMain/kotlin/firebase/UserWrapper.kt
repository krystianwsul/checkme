package com.krystianwsul.common.firebase

import kotlin.jvm.JvmOverloads

class UserWrapper @JvmOverloads constructor(
        val friendOf: MutableMap<String, Boolean> = mutableMapOf(),
        val userData: UserJson = UserJson(),
        var defaultReminder: Boolean = true,
        var defaultTab: Int = 0)
