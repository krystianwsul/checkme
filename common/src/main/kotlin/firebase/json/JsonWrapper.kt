package com.krystianwsul.common.firebase.json

import com.krystianwsul.common.firebase.json.projects.SharedProjectJson
import kotlinx.serialization.Serializable

@Serializable
data class JsonWrapper @JvmOverloads constructor(
    var projectJson: SharedProjectJson = SharedProjectJson(),
) : Parsable, DeepCopy<JsonWrapper> {

    override fun deepCopy() = deepCopy(serializer(), this)
}