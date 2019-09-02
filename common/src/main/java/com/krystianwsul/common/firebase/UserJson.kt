package com.krystianwsul.common.firebase

import kotlinx.serialization.Serializable

@Serializable
data class UserJson @JvmOverloads constructor(
        val email: String = "",
        var name: String = "",
        val tokens: MutableMap<String, String?> = mutableMapOf(),
        var photoUrl: String? = null)