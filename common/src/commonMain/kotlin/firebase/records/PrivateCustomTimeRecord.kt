package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId


class PrivateCustomTimeRecord(
        create: Boolean,
        override val id: RemoteCustomTimeId.Private,
        override val customTimeJson: PrivateCustomTimeJson,
        override val remoteProjectRecord: RemotePrivateProjectRecord
) : CustomTimeRecord<RemoteCustomTimeId.Private, ProjectKey.Private>(create) {

    constructor(
            id: RemoteCustomTimeId.Private,
            remoteProjectRecord: RemotePrivateProjectRecord,
            customTimeJson: PrivateCustomTimeJson
    ) : this(false, id, customTimeJson, remoteProjectRecord)

    constructor(
            remoteProjectRecord: RemotePrivateProjectRecord,
            customTimeJson: PrivateCustomTimeJson
    ) : this(true, remoteProjectRecord.getCustomTimeRecordId(), customTimeJson, remoteProjectRecord)

    override val customTimeKey = CustomTimeKey.Private(remoteProjectRecord.id, id)

    override val createObject get() = customTimeJson

    override fun deleteFromParent() = check(remoteProjectRecord.customTimeRecords.remove(id) == this)

    override fun mine(userInfo: UserInfo) = true

    var current by Committer(customTimeJson::current)
    var endTime by Committer(customTimeJson::endTime)
}
