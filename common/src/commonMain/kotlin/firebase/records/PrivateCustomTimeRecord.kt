package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey


class PrivateCustomTimeRecord(
        create: Boolean,
        override val id: CustomTimeId.Private,
        override val customTimeJson: PrivateCustomTimeJson,
        override val remoteProjectRecord: RemotePrivateProjectRecord
) : CustomTimeRecord<CustomTimeId.Private, ProjectKey.Private>(create) {

    constructor(
            id: CustomTimeId.Private,
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
