package com.krystianwsul.common.firebase

class UserJson @JvmOverloads constructor(
        val email: String = "",
        var name: String = "",
        val tokens: MutableMap<String, String?> = mutableMapOf(),
        var photoUrl: String? = null)