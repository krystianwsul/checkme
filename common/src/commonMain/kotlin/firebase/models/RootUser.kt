package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.RootUserProperties
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeId


open class RootUser(private val remoteRootUserRecord: RootUserRecord) : RootUserProperties by remoteRootUserRecord {

    private val _customTimes = remoteRootUserRecord.customTimeRecords
            .mapValues { Time.Custom.User(this, it.value) }
            .toMutableMap()

    val customTimes: Map<CustomTimeId.User, Time.Custom.User> get() = _customTimes

    fun deleteCustomTime(customTime: Time.Custom.User) {
        check(_customTimes.containsKey(customTime.id))

        _customTimes.remove(customTime.id)
    }
}