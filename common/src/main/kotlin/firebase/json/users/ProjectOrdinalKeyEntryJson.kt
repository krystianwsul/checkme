package com.krystianwsul.common.firebase.json.users

import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class ProjectOrdinalKeyEntryJson @JvmOverloads constructor(
    val taskInfoJson: TaskInfoJson? = null,
    val instanceDateOrDayOfWeek: String = "",
    val instanceTime: String = "",
) : Parcelable {

    @Serializable
    @Parcelize
    data class TaskInfoJson @JvmOverloads constructor(
        val taskKey: String = "",
        val scheduleDateTimePairJson: DateTimePairJson? = null,
    ) : Parcelable {

        @Serializable
        @Parcelize
        data class DateTimePairJson @JvmOverloads constructor(val date: String = "", val time: String = "") : Parcelable
    }
}