package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.MyUserProperties
import com.krystianwsul.common.firebase.json.customtimes.UserCustomTimeJson
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.customtime.MyUserCustomTime
import com.krystianwsul.common.firebase.records.MyUserRecord
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.UserKey
import kotlinx.coroutines.flow.MutableSharedFlow


class MyUser(private val remoteMyUserRecord: MyUserRecord, private val rootModelChangeManager: RootModelChangeManager) :
    RootUser(remoteMyUserRecord),
    MyUserProperties by remoteMyUserRecord {

    override val _customTimes = remoteMyUserRecord.customTimeRecords
        .mapValues { MyUserCustomTime(this, it.value, rootModelChangeManager) }
        .toMutableMap()

    override val customTimes: Map<CustomTimeId.User, MyUserCustomTime> get() = _customTimes

    override var photoUrl
        get() = super.photoUrl
        set(value) {
            remoteMyUserRecord.photoUrl = value
        }

    val friendChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun removeFriend(userKey: UserKey) {
        super.removeFriend(userKey)

        friendChanges.tryEmit(Unit)
    }

    fun newCustomTime(customTimeJson: UserCustomTimeJson): MyUserCustomTime {
        val userCustomTimeRecord = remoteMyUserRecord.newCustomTimeRecord(customTimeJson)
        val userCustomTime = MyUserCustomTime(this, userCustomTimeRecord, rootModelChangeManager)
        check(!customTimes.containsKey(userCustomTime.id))

        _customTimes[userCustomTime.id] = userCustomTime

        return userCustomTime
    }
}
