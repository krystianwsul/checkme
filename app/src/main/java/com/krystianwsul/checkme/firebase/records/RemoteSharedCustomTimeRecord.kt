package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.firebase.json.SharedCustomTimeJson


class RemoteSharedCustomTimeRecord : RemoteCustomTimeRecord {

    override val remoteProjectRecord: RemoteSharedProjectRecord

    override val customTimeJson: SharedCustomTimeJson

    val ownerId get() = customTimeJson.ownerId

    constructor(id: String, remoteProjectRecord: RemoteSharedProjectRecord, customTimeJson: SharedCustomTimeJson) : super(id) {
        this.remoteProjectRecord = remoteProjectRecord
        this.customTimeJson = customTimeJson
    }

    constructor(remoteProjectRecord: RemoteSharedProjectRecord, customTimeJson: SharedCustomTimeJson) : super(remoteProjectRecord) {
        this.remoteProjectRecord = remoteProjectRecord
        this.customTimeJson = customTimeJson
    }

    override val createObject get() = customTimeJson

    override fun deleteFromParent() = check(remoteProjectRecord.remoteCustomTimeRecords.remove(id) == this)
}
