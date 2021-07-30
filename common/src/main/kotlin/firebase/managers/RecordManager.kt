package com.krystianwsul.common.firebase.managers

interface RecordManager {

    fun save(values: MutableMap<String, Any?>)
}