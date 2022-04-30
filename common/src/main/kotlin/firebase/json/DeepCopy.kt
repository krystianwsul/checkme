package com.krystianwsul.common.firebase.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

interface DeepCopy<T : DeepCopy<T>> : Parsable {

    companion object {

        private val deepCopyJson = Json
    }

    val serializer: KSerializer<T>

    fun deepCopy(): T

    fun deepCopy(value: T) = deepCopyJson.run { decodeFromJsonElement(serializer, encodeToJsonElement(serializer, value)) }
}