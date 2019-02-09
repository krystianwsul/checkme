package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.json.SharedCustomTimeJson
import com.krystianwsul.checkme.utils.RemoteCustomTimeId


class RemoteSharedCustomTimeRecord : RemoteCustomTimeRecord<RemoteCustomTimeId.Shared> {

    override val id: RemoteCustomTimeId.Shared
    override val remoteProjectRecord: RemoteSharedProjectRecord
    override val customTimeJson: SharedCustomTimeJson

    val ownerId get() = customTimeJson.ownerId

    constructor(id: RemoteCustomTimeId.Shared, remoteProjectRecord: RemoteSharedProjectRecord, customTimeJson: SharedCustomTimeJson) : super(false) {
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

    var ownerKey: String
        get() = customTimeJson.ownerKey
        set(value) {
            check(value.isNotEmpty())

            if (customTimeJson.ownerKey == value)
                return

            customTimeJson.ownerKey = value
            addValue("$key/ownerKey", value)
        }

    var privateKey: RemoteCustomTimeId?
        get() = customTimeJson.privateKey.takeIf { it.isNotEmpty() }?.let { RemoteCustomTimeId.Private(customTimeJson.privateKey) }
        set(value) {
            checkNotNull(value)

            if (customTimeJson.privateKey == value.value)
                return

            customTimeJson.privateKey = value.value
            addValue("$key/privateKey", value.value)
        }
}
