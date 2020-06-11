package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.ShowGroupViewModel
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.time.TimeStamp

@Synchronized
fun DomainFactory.getShowGroupData(timeStamp: TimeStamp): ShowGroupViewModel.Data {
    MyCrashlytics.log("DomainFactory.getShowGroupData")

    val now = ExactTimeStamp.now

    val date = timeStamp.date
    val dayOfWeek = date.dayOfWeek
    val hourMinute = timeStamp.hourMinute

    val time =
        getCurrentRemoteCustomTimes(now).firstOrNull { it.getHourMinute(dayOfWeek) == hourMinute }
            ?: Time.Normal(hourMinute)

    val displayText = DateTime(date, time).getDisplayText()

    return ShowGroupViewModel.Data(displayText, getGroupListData(timeStamp, now))
}