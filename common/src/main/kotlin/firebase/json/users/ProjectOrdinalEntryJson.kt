package com.krystianwsul.common.firebase.json.users

import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
@Parcelize
data class ProjectOrdinalEntryJson @JvmOverloads constructor(
    val keyEntries: Map<String, ProjectOrdinalKeyEntryJson> = mapOf(),
    val ordinal: Double = 0.0,
    val ordinalString: String? = null,
    val updated: Long = 0,
) : Parcelable