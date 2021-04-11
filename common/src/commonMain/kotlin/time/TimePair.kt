package com.krystianwsul.common.time

import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import com.krystianwsul.common.utils.Serializable

@Parcelize
data class TimePair(
        val customTimeKey: CustomTimeKey.Project<*>?, // todo customtime timepair
        val hourMinute: HourMinute?,
) : Parcelable, Serializable {

    constructor(customTimeKey: CustomTimeKey.Project<*>) : this(customTimeKey, null)

    constructor(hourMinute: HourMinute) : this(null, hourMinute)

    init {
        check((customTimeKey == null) != (hourMinute == null))
    }
}
