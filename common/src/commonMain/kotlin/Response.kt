package com.krystianwsul.common

import kotlinx.serialization.Serializable

@Serializable
data class Response(val results: List<Result> = listOf()) {

    @Serializable
    data class Result(
            val message_id: String? = null,
            val error: String? = null
    )
}