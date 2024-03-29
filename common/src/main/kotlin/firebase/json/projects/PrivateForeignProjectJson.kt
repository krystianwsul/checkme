package com.krystianwsul.common.firebase.json.projects

import com.krystianwsul.common.firebase.json.DeepCopy
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class PrivateForeignProjectJson @JvmOverloads constructor(
    override val ownerName: String = "",
    override val rootTaskIds: MutableMap<String, Boolean> = mutableMapOf(),
) : ForeignProjectJson, PrivateProjectJson, DeepCopy<PrivateForeignProjectJson> {

    override fun deepCopy() = deepCopy(serializer(), this)
}