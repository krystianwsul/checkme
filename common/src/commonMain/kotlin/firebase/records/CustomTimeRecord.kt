package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.json.CustomTimeJson
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType


abstract class CustomTimeRecord<T : ProjectType>(create: Boolean) : RemoteRecord(create) {

    companion object {

        const val CUSTOM_TIMES = "customTimes"
    }

    abstract val id: CustomTimeId.Project<T>
    abstract val customTimeKey: CustomTimeKey.Project<T>
    protected abstract val customTimeJson: CustomTimeJson
    protected abstract val projectRecord: ProjectRecord<T>

    var name by Committer({ customTimeJson::name })

    var sundayHour by Committer({ customTimeJson::sundayHour })
    var sundayMinute by Committer({ customTimeJson::sundayMinute })

    var mondayHour by Committer({ customTimeJson::mondayHour })
    var mondayMinute by Committer({ customTimeJson::mondayMinute })

    var tuesdayHour by Committer({ customTimeJson::tuesdayHour })
    var tuesdayMinute by Committer({ customTimeJson::tuesdayMinute })

    var wednesdayHour by Committer({ customTimeJson::wednesdayHour })
    var wednesdayMinute by Committer({ customTimeJson::wednesdayMinute })

    var thursdayHour by Committer({ customTimeJson::thursdayHour })
    var thursdayMinute by Committer({ customTimeJson::thursdayMinute })

    var fridayHour by Committer({ customTimeJson::fridayHour })
    var fridayMinute by Committer({ customTimeJson::fridayMinute })

    var saturdayHour by Committer({ customTimeJson::saturdayHour })
    var saturdayMinute by Committer({ customTimeJson::saturdayMinute })

    val projectId get() = projectRecord.projectKey

    override val key get() = projectRecord.childKey + "/" + CUSTOM_TIMES + "/" + id

    abstract fun mine(userInfo: UserInfo): Boolean

    fun setHourMinute(dayOfWeek: DayOfWeek, hourMinute: HourMinute) {
        when (dayOfWeek) {
            DayOfWeek.SUNDAY -> {
                sundayHour = hourMinute.hour
                sundayMinute = hourMinute.minute
            }
            DayOfWeek.MONDAY -> {
                mondayHour = hourMinute.hour
                mondayMinute = hourMinute.minute
            }
            DayOfWeek.TUESDAY -> {
                tuesdayHour = hourMinute.hour
                tuesdayMinute = hourMinute.minute
            }
            DayOfWeek.WEDNESDAY -> {
                wednesdayHour = hourMinute.hour
                wednesdayMinute = hourMinute.minute
            }
            DayOfWeek.THURSDAY -> {
                thursdayHour = hourMinute.hour
                thursdayMinute = hourMinute.minute
            }
            DayOfWeek.FRIDAY -> {
                fridayHour = hourMinute.hour
                fridayMinute = hourMinute.minute
            }
            DayOfWeek.SATURDAY -> {
                saturdayHour = hourMinute.hour
                saturdayMinute = hourMinute.minute
            }
        }
    }
}
