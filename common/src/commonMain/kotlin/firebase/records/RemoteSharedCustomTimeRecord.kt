package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.domain.UserInfo

import com.krystianwsul.common.firebase.json.SharedCustomTimeJson
import com.krystianwsul.common.utils.RemoteCustomTimeId


class RemoteSharedCustomTimeRecord : RemoteCustomTimeRecord<RemoteCustomTimeId.Shared> {

    override val id: RemoteCustomTimeId.Shared
    override val remoteProjectRecord: RemoteSharedProjectRecord
    override val customTimeJson: SharedCustomTimeJson

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

    override fun mine(userInfo: UserInfo) = ownerKey == userInfo.key

    var ownerKey: String?
        get() = customTimeJson.ownerKey
        set(value) {
            check(!value.isNullOrEmpty())

            if (customTimeJson.ownerKey == value)
                return

            customTimeJson.ownerKey = value
            addValue("$key/ownerKey", value)
        }

    var privateKey: RemoteCustomTimeId.Private?
        get() = customTimeJson.privateKey.takeUnless { it.isNullOrEmpty() }?.let { RemoteCustomTimeId.Private(it) }
        set(value) {
            checkNotNull(value)

            if (customTimeJson.privateKey == value.value)
                return

            customTimeJson.privateKey = value.value
            addValue("$key/privateKey", value.value)
        }
}
