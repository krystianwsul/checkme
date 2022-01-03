package com.krystianwsul.common.firebase.json.users

data class ProjectOrdinalEntryJson @JvmOverloads constructor(
    val keyEntries: List<ProjectOrdinalKeyEntryJson> = listOf(),
    val ordinal: Double = 0.0,
    val updated: Long = 0,
)