package com.krystianwsul.common.firebase.models.customtime

import com.krystianwsul.common.firebase.MyCustomTime
import com.krystianwsul.common.firebase.models.MyUser
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.records.customtime.UserCustomTimeRecord
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.Time

class MyUserCustomTime(
    myUser: MyUser,
    userCustomTimeRecord: UserCustomTimeRecord,
    private val rootModelChangeManager: RootModelChangeManager,
) : Time.Custom.User(myUser, userCustomTimeRecord), MyCustomTime {

    override var endExactTimeStamp
        get() = super.endExactTimeStamp
        set(value) {
            check((value == null) != (customTimeRecord.endTime == null))

            customTimeRecord.endTime = value?.long
        }

    fun setHourMinute(dayOfWeek: DayOfWeek, hourMinute: HourMinute) {
        customTimeRecord.setHourMinute(dayOfWeek, hourMinute)

        rootModelChangeManager.customTimesInvalidatableManager.invalidate()
    }

    fun setName(name: String) {
        customTimeRecord.name = name
    }
}