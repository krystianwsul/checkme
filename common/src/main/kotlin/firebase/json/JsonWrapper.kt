package com.krystianwsul.common.firebase.json

import com.krystianwsul.common.firebase.json.projects.SharedOwnedProjectJson
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class JsonWrapper @JvmOverloads constructor(
    var projectJson: SharedOwnedProjectJson = SharedOwnedProjectJson(),
) : Parsable, DeepCopy<JsonWrapper> {

    companion object {

        const val PROJECT_JSON = "projectJson"
    }

    override fun deepCopy() = deepCopy(serializer(), this)
}