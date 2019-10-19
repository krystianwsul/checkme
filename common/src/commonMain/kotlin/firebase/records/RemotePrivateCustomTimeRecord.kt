package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.domain.UserInfo

import com.krystianwsul.common.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.common.utils.RemoteCustomTimeId


class RemotePrivateCustomTimeRecord : RemoteCustomTimeRecord<RemoteCustomTimeId.Private, PrivateCustomTimeJson> {

    override val id: RemoteCustomTimeId.Private
    override val remoteProjectRecord: RemotePrivateProjectRecord

    constructor(id: RemoteCustomTimeId.Private, remoteProjectRecord: RemotePrivateProjectRecord, customTimeJson: PrivateCustomTimeJson) : super(false, customTimeJson) {
        this.id = id
        this.remoteProjectRecord = remoteProjectRecord
    }

    constructor(remoteProjectRecord: RemotePrivateProjectRecord, customTimeJson: PrivateCustomTimeJson) : super(true, customTimeJson) {
        id = remoteProjectRecord.getCustomTimeRecordId()
        this.remoteProjectRecord = remoteProjectRecord
    }

    override val createObject get() = customTimeJson

    override fun deleteFromParent() = check(remoteProjectRecord.remoteCustomTimeRecords.remove(id) == this)

    override fun mine(userInfo: UserInfo) = true

    var current: Boolean
        get() = customTimeJson.current
        set(value) {
            if (customTimeJson.current == value)
                return

            customTimeJson.current = value
            addValue("$key/current", value)
        }

    var endTime
        get() = customTimeJson.endTime
        set(value) {
            if (value == customTimeJson.endTime)
                return

            customTimeJson.endTime = value
            addValue("$key/endTime", value)
        }
}
