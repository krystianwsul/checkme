package com.krystianwsul.common.firebase.json.projects

import com.krystianwsul.common.firebase.json.DeepCopy
import com.krystianwsul.common.firebase.json.users.UserJson
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class SharedForeignProjectJson @JvmOverloads constructor(
    override var name: String = "",
    override val users: Map<String, UserJson> = mapOf(),
    override val rootTaskIds: MutableMap<String, Boolean> = mutableMapOf(),
) : ForeignProjectJson, SharedProjectJson, DeepCopy<SharedForeignProjectJson> {

    override fun deepCopy() = deepCopy(serializer(), this)
}