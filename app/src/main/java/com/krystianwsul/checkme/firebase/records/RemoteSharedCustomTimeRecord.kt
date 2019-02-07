package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.json.SharedCustomTimeJson


class RemoteSharedCustomTimeRecord : RemoteCustomTimeRecord {

    override val id: String
    override val remoteProjectRecord: RemoteSharedProjectRecord
    override val customTimeJson: SharedCustomTimeJson

    val ownerId get() = customTimeJson.ownerId

    constructor(id: String, remoteProjectRecord: RemoteSharedProjectRecord, customTimeJson: SharedCustomTimeJson) : super(false) {
        this.id = id
        this.remoteProjectRecord = remoteProjectRecord
        this.customTimeJson = customTimeJson
    }

    constructor(remoteProjectRecord: RemoteSharedProjectRecord, customTimeJson: SharedCustomTimeJson) : super(true) {
        id = remoteProjectRecord.getCustomTimeRecordId()
        this.remoteProjectRecord = remoteProjectRecord
        this.customTimeJson = customTimeJson
    }

    override val createObject get() = customTimeJson

    override fun deleteFromParent() = check(remoteProjectRecord.remoteCustomTimeRecords.remove(id) == this)

    override fun mine(domainFactory: DomainFactory) = ownerId == domainFactory.uuid
}
