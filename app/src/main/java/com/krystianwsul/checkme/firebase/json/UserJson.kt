package com.krystianwsul.checkme.firebase.json

class UserJson @JvmOverloads constructor(
        val email: String = "",
        var name: String = "",
        val tokens: MutableMap<String, String?> = mutableMapOf())