package com.krystianwsul.checkme.firebase.json

import com.google.firebase.database.IgnoreExtraProperties

import junit.framework.Assert

@IgnoreExtraProperties
class JsonWrapper(val recordOf: MutableMap<String, Boolean> = mutableMapOf(), val projectJson: ProjectJson = ProjectJson()) {

    constructor(recordOf: Set<String>, projectJson: ProjectJson) : this(recordOf.associateBy({ it }, { true }).toMutableMap(), projectJson)

    fun updateRecordOf(add: Set<String>, remove: Set<String>) {
        Assert.assertTrue(add.none { remove.contains(it) })
        Assert.assertTrue(add.none { recordOf.containsKey(it) })

        Assert.assertTrue(remove.none { add.contains(it) })
        Assert.assertTrue(remove.all { recordOf.containsKey(it) })

        for (key in remove)
            recordOf.remove(key)

        for (key in add)
            recordOf[key] = true
    }
}
