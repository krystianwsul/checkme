package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.UserCustomTimeRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.Endable

class MyUserCustomTime(
        private val myUser: MyUser,
        userCustomTimeRecord: UserCustomTimeRecord,
) : Time.Custom.User(myUser, userCustomTimeRecord), Endable {

    override var endExactTimeStamp
        get() = customTimeRecord.endTime?.let { ExactTimeStamp.Local(it) }
        set(value) {
            check((value == null) != (customTimeRecord.endTime == null))

            customTimeRecord.endTime = value?.long
        }
}