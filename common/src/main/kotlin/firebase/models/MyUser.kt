package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.MyUserProperties
import com.krystianwsul.common.firebase.json.customtimes.UserCustomTimeJson
import com.krystianwsul.common.firebase.models.customtime.MyUserCustomTime
import com.krystianwsul.common.firebase.records.MyUserRecord
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.UserKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking


class MyUser(private val remoteMyUserRecord: MyUserRecord) :
        RootUser(remoteMyUserRecord),
        MyUserProperties by remoteMyUserRecord {

    override val _customTimes = remoteMyUserRecord.customTimeRecords
            .mapValues { MyUserCustomTime(this, it.value) }
            .toMutableMap()

    override val customTimes: Map<CustomTimeId.User, MyUserCustomTime> get() = _customTimes

    override var photoUrl
        get() = super.photoUrl
        set(value) {
            remoteMyUserRecord.photoUrl = value
        }

    val friendChanges = MutableSharedFlow<Unit>()

    override fun removeFriend(userKey: UserKey) {
        super.removeFriend(userKey)

        runBlocking { friendChanges.emit(Unit) }
    }

    fun newCustomTime(customTimeJson: UserCustomTimeJson): MyUserCustomTime {
        val userCustomTimeRecord = remoteMyUserRecord.newCustomTimeRecord(customTimeJson)
        val userCustomTime = MyUserCustomTime(this, userCustomTimeRecord)
        check(!customTimes.containsKey(userCustomTime.id))

        _customTimes[userCustomTime.id] = userCustomTime

        return userCustomTime
    }
}
