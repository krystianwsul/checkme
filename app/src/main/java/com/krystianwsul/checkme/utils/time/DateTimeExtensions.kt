package com.krystianwsul.checkme.utils.time

import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.firebase.RemoteProject
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.RemoteCustomTimeId
import java.util.*
import org.joda.time.DateTime as DateTimeJoda

fun Date.getDisplayText(): String {
    val todayCalendar = Calendar.getInstance()
    val todayDate = Date(todayCalendar.toDateTimeTz())

    val yesterdayCalendar = Calendar.getInstance()
    yesterdayCalendar.add(Calendar.DATE, -1)
    val yesterdayDate = Date(yesterdayCalendar.toDateTimeTz())

    val tomorrowCalendar = Calendar.getInstance()
    tomorrowCalendar.add(Calendar.DATE, 1)
    val tomorrowDate = Date(tomorrowCalendar.toDateTimeTz())

    val context = MyApplication.context

    return when (this) {
        todayDate -> context.getString(R.string.today)
        yesterdayDate -> context.getString(R.string.yesterday)
        tomorrowDate -> context.getString(R.string.tomorrow)
        else -> dayOfWeek.toString() + ", " + toString()
    }
}

fun Calendar.toDateTimeSoy() = DateTimeSoy.fromUnix(timeInMillis)

fun Calendar.toDateTimeTz() = toDateTimeSoy().local

val Date.calendar get() = GregorianCalendar(year, month - 1, day)

val ExactTimeStamp.calendar: Calendar get() = Calendar.getInstance().apply { timeInMillis = long }

fun DateTimeJoda.toExactTimeStamp() = ExactTimeStamp(millis)

val TimeStamp.calendar: Calendar get() = Calendar.getInstance().apply { timeInMillis = long }

fun DateTime.getDisplayText() = date.getDisplayText() + ", " + time.toString()

fun <T : RemoteCustomTimeId> TimePair.destructureRemote(remoteProject: RemoteProject<T>): Triple<T?, Int?, Int?> {
    val remoteCustomTimeId: T?
    val hour: Int?
    val minute: Int?
    if (customTimeKey != null) {
        check(hourMinute == null)

        remoteCustomTimeId = remoteProject.getRemoteCustomTimeKey(customTimeKey!!).remoteCustomTimeId

        hour = null
        minute = null
    } else {
        remoteCustomTimeId = null
        hour = hourMinute!!.hour
        minute = hourMinute!!.minute
    }

    return Triple(remoteCustomTimeId, hour, minute)
}