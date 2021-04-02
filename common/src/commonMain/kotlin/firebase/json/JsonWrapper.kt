package com.krystianwsul.common.firebase.json

import kotlin.jvm.JvmOverloads

@Serializable
data class JsonWrapper @JvmOverloads constructor(
        var projectJson: SharedProjectJson = SharedProjectJson(),
)