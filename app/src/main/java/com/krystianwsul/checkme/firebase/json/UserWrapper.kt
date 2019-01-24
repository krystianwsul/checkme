package com.krystianwsul.checkme.firebase.json

class UserWrapper @JvmOverloads constructor(
        val friendOf: MutableMap<String, Boolean> = mutableMapOf(),
        val userData: UserJson = UserJson())
