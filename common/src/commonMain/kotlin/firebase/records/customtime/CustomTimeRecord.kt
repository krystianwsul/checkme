package com.krystianwsul.common.firebase.records.customtime

import com.krystianwsul.common.firebase.json.customtimes.CustomTimeJson
import com.krystianwsul.common.firebase.records.RemoteRecord
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey


abstract class CustomTimeRecord(create: Boolean) : RemoteRecord(create) {

    companion object {

        const val CUSTOM_TIMES = "customTimes"
    }

    abstract val id: CustomTimeId
    abstract val customTimeKey: CustomTimeKey
    protected abstract val customTimeJson: CustomTimeJson

    abstract val name: String

    abstract val sundayHour: Int
    abstract val sundayMinute: Int

    abstract val mondayHour: Int
    abstract val mondayMinute: Int

    abstract val tuesdayHour: Int
    abstract val tuesdayMinute: Int

    abstract val wednesdayHour: Int
    abstract val wednesdayMinute: Int

    abstract val thursdayHour: Int
    abstract val thursdayMinute: Int

    abstract val fridayHour: Int
    abstract val fridayMinute: Int

    abstract val saturdayHour: Int
    abstract val saturdayMinute: Int
}
