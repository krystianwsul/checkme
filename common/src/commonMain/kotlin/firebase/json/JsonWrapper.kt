package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
class JsonWrapper @JvmOverloads constructor(
        var projectJson: SharedProjectJson = SharedProjectJson()
)