package com.krystianwsul.common.time

import com.krystianwsul.common.firebase.json.users.ProjectOrdinalKeyEntryJson
import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import com.krystianwsul.common.utils.Serializable

@Parcelize
data class DateTimePair(val date: Date, val timePair: TimePair) : Parcelable, Serializable {

    fun toJson() = ProjectOrdinalKeyEntryJson.TaskInfoJson.DateTimePairJson(
        date.toJson(),
        timePair
            .toJsonTime()
            .toJson()
    )
}