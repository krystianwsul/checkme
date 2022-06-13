package com.krystianwsul.common.utils

import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.TimePair

@Parcelize
data class InstanceKey(val taskKey: TaskKey, val instanceScheduleKey: InstanceScheduleKey) : Parcelable, Serializable {

    companion object {

        fun fromJson(
            projectCustomTimeIdAndKeyProvider: JsonTime.ProjectCustomTimeIdAndKeyProvider,
            json: String,
        ): InstanceKey {
            val (taskKeyString, instanceScheduleKeyString) = json.split(';')

            val taskKey = TaskKey.fromShortcut(taskKeyString)
            val instanceScheduleKey =
                InstanceRecord.stringToScheduleKey(projectCustomTimeIdAndKeyProvider, instanceScheduleKeyString)

            return InstanceKey(taskKey, instanceScheduleKey)
        }
    }

    constructor(taskKey: TaskKey, scheduleDate: Date, scheduleTimePair: TimePair) :
            this(taskKey, InstanceScheduleKey(scheduleDate, scheduleTimePair))

    fun toJson(): String {
        val taskKeyString = taskKey.toShortcut()
        val instanceScheduleKeyString = InstanceRecord.scheduleKeyToString(instanceScheduleKey)

        return "$taskKeyString;$instanceScheduleKeyString"
    }
}
