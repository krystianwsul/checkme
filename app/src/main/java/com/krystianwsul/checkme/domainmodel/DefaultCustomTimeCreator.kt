package com.krystianwsul.checkme.domainmodel

import androidx.annotation.StringRes
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.common.firebase.json.customtimes.PrivateCustomTimeJson
import com.krystianwsul.common.firebase.json.customtimes.UserCustomTimeJson
import com.krystianwsul.common.firebase.models.MyUser
import com.krystianwsul.common.firebase.models.project.PrivateProject
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.Time

object DefaultCustomTimeCreator {

    private fun createCustomTime(
            privateProject: PrivateProject,
            myUser: MyUser,
            @StringRes nameRes: Int,
            hourMinute: HourMinute,
    ) {
        val name = MyApplication.context.getString(nameRes)

        if (Time.Custom.User.WRITE_USER_CUSTOM_TIMES) {
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
        } else {
            privateProject.newRemoteCustomTime(
                    PrivateCustomTimeJson(
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
    }

    fun createDefaultCustomTimes(privateProject: PrivateProject, myUser: MyUser) {
        createCustomTime(privateProject, myUser, R.string.morning, HourMinute(9, 0))
        createCustomTime(privateProject, myUser, R.string.afternoon, HourMinute(13, 0))
        createCustomTime(privateProject, myUser, R.string.evening, HourMinute(18, 0))
    }
}