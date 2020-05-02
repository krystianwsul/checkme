package com.krystianwsul.common.firebase.managers

interface RecordManager {

    val isSaved: Boolean

    val savedList get() = listOfNotNull(this::class.simpleName.takeIf { isSaved })

    fun save(values: MutableMap<String, Any?>)
}