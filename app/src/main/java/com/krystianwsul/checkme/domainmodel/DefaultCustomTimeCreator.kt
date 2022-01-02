package com.krystianwsul.checkme.domainmodel

import androidx.annotation.StringRes
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.common.firebase.json.customtimes.UserCustomTimeJson
import com.krystianwsul.common.firebase.models.users.MyUser
import com.krystianwsul.common.time.HourMinute

object DefaultCustomTimeCreator {

    private fun createCustomTime(
        myUser: MyUser,
        @StringRes nameRes: Int,
        hourMinute: HourMinute,
    ) {
        val name = MyApplication.context.getString(nameRes)

        myUser.newCustomTime(
            UserCustomTimeJson(
                name,
                hourMinute.hour,
                hourMinute.minute,
                hourMinute.hour,
                hourMinute.minute,
                hourMinute.hour,
                hourMinute.minute,
                hourMinute.hour,
                hourMinute.minute,
                hourMinute.hour,
                hourMinute.minute,
                hourMinute.hour,
                hourMinute.minute,
                hourMinute.hour,
                hourMinute.minute,
            )
        )
    }

    fun createDefaultCustomTimes(myUser: MyUser) {
        if (myUser.customTimes.isEmpty()) {
            createCustomTime(myUser, R.string.morning, HourMinute(9, 0))
            createCustomTime(myUser, R.string.afternoon, HourMinute(13, 0))
            createCustomTime(myUser, R.string.evening, HourMinute(18, 0))
        }
    }
}