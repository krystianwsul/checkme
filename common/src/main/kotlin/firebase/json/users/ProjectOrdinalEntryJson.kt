package com.krystianwsul.common.firebase.json.users

import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class ProjectOrdinalEntryJson @JvmOverloads constructor(
    val keyEntries: List<ProjectOrdinalKeyEntryJson> = listOf(),
    val ordinal: Double = 0.0,
    val updated: Long = 0,
) : Parcelable