package com.krystianwsul.common.firebase.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

interface DeepCopy<T : DeepCopy<T>> : Parsable {

    companion object {

        private val deepCopyJson = Json
    }

    fun deepCopy(): T

    fun deepCopy(serializer: KSerializer<T>, value: T) =
        deepCopyJson.run { decodeFromJsonElement(serializer, encodeToJsonElement(serializer, value)) }
}