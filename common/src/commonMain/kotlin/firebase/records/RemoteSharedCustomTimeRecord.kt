package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.json.SharedCustomTimeJson
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.UserKey
import kotlin.properties.Delegates.observable


class RemoteSharedCustomTimeRecord : RemoteCustomTimeRecord<RemoteCustomTimeId.Shared, SharedCustomTimeJson> {

    override val id: RemoteCustomTimeId.Shared
    override val remoteProjectRecord: RemoteSharedProjectRecord

    constructor(id: RemoteCustomTimeId.Shared, remoteProjectRecord: RemoteSharedProjectRecord, customTimeJson: SharedCustomTimeJson) : super(false, customTimeJson) {
        this.id = id
        this.remoteProjectRecord = remoteProjectRecord
    }

    constructor(remoteProjectRecord: RemoteSharedProjectRecord, customTimeJson: SharedCustomTimeJson) : super(true, customTimeJson) {
        id = remoteProjectRecord.getCustomTimeRecordId()
        this.remoteProjectRecord = remoteProjectRecord
    }

    override val createObject get() = customTimeJson

    override fun deleteFromParent() = check(remoteProjectRecord.remoteCustomTimeRecords.remove(id) == this)

    override fun mine(userInfo: UserInfo) = ownerKey == userInfo.key

    var ownerKey by observable(customTimeJson.ownerKey?.let { UserKey(it) }) { _, _, newValue ->
        customTimeJson.ownerKey = newValue?.key
    }

    var privateKey: RemoteCustomTimeId.Private?
        get() = customTimeJson.privateKey
                .takeUnless { it.isNullOrEmpty() }
                ?.let { RemoteCustomTimeId.Private(it) }
        set(value) {
            checkNotNull(value)

            setProperty(customTimeJson::privateKey, value.value)
        }
}
