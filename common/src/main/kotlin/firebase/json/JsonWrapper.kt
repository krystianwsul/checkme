package com.krystianwsul.common.firebase.json

import com.krystianwsul.common.firebase.json.projects.SharedOwnedProjectJson
import kotlinx.serialization.Serializable

@Serializable
data class JsonWrapper @JvmOverloads constructor(
    var projectJson: SharedOwnedProjectJson = SharedOwnedProjectJson(),
) : Parsable, DeepCopy<JsonWrapper> {

    override fun deepCopy() = deepCopy(serializer(), this)
}