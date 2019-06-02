package com.krystianwsul.checkme.firebase.json

import com.krystianwsul.common.firebase.UserJson

class UserWrapper @JvmOverloads constructor(
        val friendOf: MutableMap<String, Boolean> = mutableMapOf(),
        val userData: UserJson = UserJson(),
        var defaultReminder: Boolean = true,
        var defaultTab: Int = 0)
