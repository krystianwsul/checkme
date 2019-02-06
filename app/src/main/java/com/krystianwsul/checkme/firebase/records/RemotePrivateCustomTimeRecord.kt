package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.firebase.json.PrivateCustomTimeJson


class RemotePrivateCustomTimeRecord : RemoteCustomTimeRecord {

    override val remoteProjectRecord: RemotePrivateProjectRecord

    override val customTimeJson: PrivateCustomTimeJson

    constructor(id: String, remoteProjectRecord: RemotePrivateProjectRecord, customTimeJson: PrivateCustomTimeJson) : super(id) {
        this.remoteProjectRecord = remoteProjectRecord
        this.customTimeJson = customTimeJson
    }

    constructor(remoteProjectRecord: RemotePrivateProjectRecord, customTimeJson: PrivateCustomTimeJson) : super(remoteProjectRecord) {
        this.remoteProjectRecord = remoteProjectRecord
        this.customTimeJson = customTimeJson
    }

    override val createObject get() = customTimeJson

    override fun deleteFromParent() = check(remoteProjectRecord.remoteCustomTimeRecords.remove(id) == this)
}
