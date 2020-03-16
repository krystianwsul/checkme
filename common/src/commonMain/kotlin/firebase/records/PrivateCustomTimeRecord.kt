package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.domain.UserInfo

import com.krystianwsul.common.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.common.utils.RemoteCustomTimeId


class PrivateCustomTimeRecord : CustomTimeRecord<RemoteCustomTimeId.Private, PrivateCustomTimeJson> {

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

    override fun deleteFromParent() = check(remoteProjectRecord.customTimeRecords.remove(id) == this)

    override fun mine(userInfo: UserInfo) = true

    var current by Committer(customTimeJson::current)
    var endTime by Committer(customTimeJson::endTime)
}
