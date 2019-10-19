package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.domain.CustomTimeRecord
import com.krystianwsul.common.domain.UserInfo

import com.krystianwsul.common.firebase.json.CustomTimeJson
import com.krystianwsul.common.utils.RemoteCustomTimeId


abstract class RemoteCustomTimeRecord<T : RemoteCustomTimeId, U : CustomTimeJson>(create: Boolean, protected val customTimeJson: U) : RemoteRecord(create), CustomTimeRecord {

    companion object {

        const val CUSTOM_TIMES = "customTimes"
    }

    abstract val id: RemoteCustomTimeId

    protected abstract val remoteProjectRecord: RemoteProjectRecord<T, *>

    override var name by Committer(customTimeJson::name)

    override var sundayHour by Committer(customTimeJson::sundayHour)
    override var sundayMinute by Committer(customTimeJson::sundayMinute)

    override var mondayHour by Committer(customTimeJson::mondayHour)
    override var mondayMinute by Committer(customTimeJson::mondayMinute)

    override var tuesdayHour by Committer(customTimeJson::tuesdayHour)
    override var tuesdayMinute by Committer(customTimeJson::tuesdayMinute)

    override var wednesdayHour by Committer(customTimeJson::wednesdayHour)
    override var wednesdayMinute by Committer(customTimeJson::wednesdayMinute)

    override var thursdayHour by Committer(customTimeJson::thursdayHour)
    override var thursdayMinute by Committer(customTimeJson::thursdayMinute)

    override var fridayHour by Committer(customTimeJson::fridayHour)
    override var fridayMinute by Committer(customTimeJson::fridayMinute)

    override var saturdayHour by Committer(customTimeJson::saturdayHour)
    override var saturdayMinute by Committer(customTimeJson::saturdayMinute)

    val projectId get() = remoteProjectRecord.id

    override val key get() = remoteProjectRecord.childKey + "/" + CUSTOM_TIMES + "/" + id

    abstract fun mine(userInfo: UserInfo): Boolean
}
