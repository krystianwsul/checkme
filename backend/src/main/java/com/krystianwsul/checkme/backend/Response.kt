package com.krystianwsul.checkme.backend

data class Response(val results: List<Result> = listOf()) {

    data class Result(
            val message_id: String? = null,
            val error: String? = null
    )
}