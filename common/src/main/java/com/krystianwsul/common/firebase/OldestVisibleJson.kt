package com.krystianwsul.common.firebase

import kotlinx.serialization.Serializable

@Serializable
class OldestVisibleJson @JvmOverloads constructor(
        var date: String? = null,
        var year: Int = 0,
        var month: Int = 0,
        var day: Int = 0) {

    companion object
}