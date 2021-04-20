package com.krystianwsul.common.firebase.json

import com.krystianwsul.common.firebase.json.projects.SharedProjectJson
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class JsonWrapper @JvmOverloads constructor(
        var projectJson: SharedProjectJson = SharedProjectJson(),
) : Parsable