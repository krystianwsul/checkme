package com.krystianwsul.common.firebase.json

import com.krystianwsul.common.utils.ProjectKey
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
class JsonWrapper @JvmOverloads constructor(
        val recordOf: MutableMap<String, Boolean> = mutableMapOf(),
        var projectJson: SharedProjectJson = SharedProjectJson()
) {

    constructor(recordOf: Set<String>, projectJson: SharedProjectJson) : this(recordOf.associateBy({ it }, { true }).toMutableMap(), projectJson)

    fun updateRecordOf(add: Set<ProjectKey.Private>, remove: Set<ProjectKey.Private>) {
        check(add.none { remove.contains(it) })
        check(add.none { recordOf.containsKey(it.key) })

        check(remove.none { add.contains(it) })
        check(remove.all { recordOf.containsKey(it.key) })

        for (key in remove)
            recordOf.remove(key.key)

        for (key in add)
            recordOf[key.key] = true
    }
}
