package com.krystianwsul.common.firebase.records.customtime

import com.krystianwsul.common.firebase.json.customtimes.UserCustomTimeJson
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey


class UserCustomTimeRecord(
        create: Boolean,
        override val id: CustomTimeId.User,
        override val customTimeJson: UserCustomTimeJson,
        private val rootUserRecord: RootUserRecord,
) : CustomTimeRecord(create) {

    constructor(
            id: CustomTimeId.User,
            rootUserRecord: RootUserRecord,
            customTimeJson: UserCustomTimeJson,
    ) : this(false, id, customTimeJson, rootUserRecord)

    constructor(
        rootUserRecord: RootUserRecord,
        customTimeJson: UserCustomTimeJson,
    ) : this(true, rootUserRecord.newCustomTimeId(), customTimeJson, rootUserRecord)

    override val key get() = rootUserRecord.key + "/" + CUSTOM_TIMES + "/" + id

    override val customTimeKey = CustomTimeKey.User(rootUserRecord.userKey, id)

    override val createObject get() = customTimeJson

    override var name by Committer({ customTimeJson::name })

    override var sundayHour by Committer({ customTimeJson::sundayHour })
    override var sundayMinute by Committer({ customTimeJson::sundayMinute })

    override var mondayHour by Committer({ customTimeJson::mondayHour })
    override var mondayMinute by Committer({ customTimeJson::mondayMinute })

    override var tuesdayHour by Committer({ customTimeJson::tuesdayHour })
    override var tuesdayMinute by Committer({ customTimeJson::tuesdayMinute })

    override var wednesdayHour by Committer({ customTimeJson::wednesdayHour })
    override var wednesdayMinute by Committer({ customTimeJson::wednesdayMinute })

    override var thursdayHour by Committer({ customTimeJson::thursdayHour })
    override var thursdayMinute by Committer({ customTimeJson::thursdayMinute })

    override var fridayHour by Committer({ customTimeJson::fridayHour })
    override var fridayMinute by Committer({ customTimeJson::fridayMinute })

    override var saturdayHour by Committer({ customTimeJson::saturdayHour })
    override var saturdayMinute by Committer({ customTimeJson::saturdayMinute })

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

    var endTime by Committer(customTimeJson::endTime)

    val privateCustomTimeId by lazy {
        customTimeJson.privateCustomTimeId?.let { CustomTimeId.Project.Private(it) }
    }

    override fun deleteFromParent() = check(rootUserRecord.customTimeRecords.remove(id) == this)
}
