package com.krystianwsul.checkme.firebase.json

import com.google.firebase.database.IgnoreExtraProperties



@IgnoreExtraProperties
class JsonWrapper(val recordOf: MutableMap<String, Boolean> = mutableMapOf(), val projectJson: ProjectJson = ProjectJson()) {

    constructor(recordOf: Set<String>, projectJson: ProjectJson) : this(recordOf.associateBy({ it }, { true }).toMutableMap(), projectJson)

    fun updateRecordOf(add: Set<String>, remove: Set<String>) {
        check(add.none { remove.contains(it) })
        check(add.none { recordOf.containsKey(it) })

        check(remove.none { add.contains(it) })
        check(remove.all { recordOf.containsKey(it) })

        for (key in remove)
            recordOf.remove(key)

        for (key in add)
            recordOf[key] = true
    }
}
