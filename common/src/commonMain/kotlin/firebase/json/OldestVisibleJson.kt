package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
class OldestVisibleJson @JvmOverloads constructor(
        var date: String? = null,
        var year: Int = 0,
        var month: Int = 0,
        var day: Int = 0) {

    companion object
}