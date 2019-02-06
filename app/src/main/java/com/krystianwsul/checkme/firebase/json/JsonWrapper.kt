package com.krystianwsul.checkme.firebase.json


class JsonWrapper @JvmOverloads constructor(
        val recordOf: MutableMap<String, Boolean> = mutableMapOf(),
        var projectJson: SharedProjectJson = SharedProjectJson()) {

    constructor(recordOf: Set<String>, projectJson: SharedProjectJson) : this(recordOf.associateBy({ it }, { true }).toMutableMap(), projectJson)

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
