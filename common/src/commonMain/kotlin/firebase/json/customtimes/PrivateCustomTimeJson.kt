package com.krystianwsul.common.firebase.json.customtimes

import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
@Parcelize
data class PrivateCustomTimeJson @JvmOverloads constructor(
        override val name: String = "",
        override val sundayHour: Int = 0,
        override val sundayMinute: Int = 0,
        override val mondayHour: Int = 0,
        override val mondayMinute: Int = 0,
        override val tuesdayHour: Int = 0,
        override val tuesdayMinute: Int = 0,
        override val wednesdayHour: Int = 0,
        override val wednesdayMinute: Int = 0,
        override val thursdayHour: Int = 0,
        override val thursdayMinute: Int = 0,
        override val fridayHour: Int = 0,
        override val fridayMinute: Int = 0,
        override val saturdayHour: Int = 0,
        override val saturdayMinute: Int = 0,
        var current: Boolean = true,
        var endTime: Long? = null,
) : CustomTimeJson, Parcelable