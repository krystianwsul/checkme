package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.domainmodel.DeviceInfo
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.common.firebase.PrivateCustomTimeJson


class RemotePrivateCustomTimeRecord : RemoteCustomTimeRecord<RemoteCustomTimeId.Private> {

    override val id: RemoteCustomTimeId.Private
    override val remoteProjectRecord: RemotePrivateProjectRecord
    override val customTimeJson: PrivateCustomTimeJson

    constructor(id: RemoteCustomTimeId.Private, remoteProjectRecord: RemotePrivateProjectRecord, customTimeJson: PrivateCustomTimeJson) : super(false) {
        this.id = id
        this.remoteProjectRecord = remoteProjectRecord
        this.customTimeJson = customTimeJson
    }

    constructor(remoteProjectRecord: RemotePrivateProjectRecord, customTimeJson: PrivateCustomTimeJson) : super(true) {
        id = remoteProjectRecord.getCustomTimeRecordId()
        this.remoteProjectRecord = remoteProjectRecord
        this.customTimeJson = customTimeJson
    }

    override val createObject get() = customTimeJson

    override fun deleteFromParent() = check(remoteProjectRecord.remoteCustomTimeRecords.remove(id) == this)

    override fun mine(deviceInfo: DeviceInfo) = true

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
