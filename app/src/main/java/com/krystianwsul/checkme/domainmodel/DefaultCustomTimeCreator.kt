package com.krystianwsul.checkme.domainmodel

import androidx.annotation.StringRes
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.common.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.common.firebase.models.PrivateProject
import com.krystianwsul.common.time.HourMinute

object DefaultCustomTimeCreator {

    private fun createCustomTime(privateProject: PrivateProject, @StringRes nameRes: Int, hourMinute: HourMinute) {
        val name = MyApplication.context.getString(nameRes)

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
                        true
                )
        )
    }

    fun createDefaultCustomTimes(privateProject: PrivateProject) {
        createCustomTime(privateProject, R.string.morning, HourMinute(9, 0))
        createCustomTime(privateProject, R.string.afternoon, HourMinute(13, 0))
        createCustomTime(privateProject, R.string.evening, HourMinute(18, 0))
    }
}