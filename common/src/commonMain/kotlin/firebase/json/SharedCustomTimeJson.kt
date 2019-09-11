package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
class SharedCustomTimeJson @JvmOverloads constructor(
        override var name: String = "",
        override var sundayHour: Int = 0,
        override var sundayMinute: Int = 0,
        override var mondayHour: Int = 0,
        override var mondayMinute: Int = 0,
        override var tuesdayHour: Int = 0,
        override var tuesdayMinute: Int = 0,
        override var wednesdayHour: Int = 0,
        override var wednesdayMinute: Int = 0,
        override var thursdayHour: Int = 0,
        override var thursdayMinute: Int = 0,
        override var fridayHour: Int = 0,
        override var fridayMinute: Int = 0,
        override var saturdayHour: Int = 0,
        override var saturdayMinute: Int = 0,
        var ownerKey: String? = null,
        var privateKey: String? = null
) : CustomTimeJson