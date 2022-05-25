package com.krystianwsul.common.firebase.json.projects

import kotlinx.serialization.Serializable

@Serializable
data class PrivateForeignProjectJson @JvmOverloads constructor(
    override val rootTaskIds: MutableMap<String, Boolean> = mutableMapOf(),
) : ForeignProjectJson, PrivateProjectJson