package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.json.SharedCustomTimeJson
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.UserKey
import kotlin.properties.Delegates.observable


class SharedCustomTimeRecord(
        create: Boolean,
        override val id: RemoteCustomTimeId.Shared,
        override val customTimeJson: SharedCustomTimeJson,
        override val remoteProjectRecord: RemoteSharedProjectRecord
) : CustomTimeRecord<RemoteCustomTimeId.Shared, ProjectKey.Shared>(create) {

    constructor(
            id: RemoteCustomTimeId.Shared,
            remoteProjectRecord: RemoteSharedProjectRecord,
            customTimeJson: SharedCustomTimeJson
    ) : this(false, id, customTimeJson, remoteProjectRecord)

    constructor(
            remoteProjectRecord: RemoteSharedProjectRecord,
            customTimeJson: SharedCustomTimeJson
    ) : this(true, remoteProjectRecord.getCustomTimeRecordId(), customTimeJson, remoteProjectRecord)

    override val customTimeKey = CustomTimeKey.Shared(remoteProjectRecord.id, id)

    override val createObject get() = customTimeJson

    override fun deleteFromParent() = check(remoteProjectRecord.customTimeRecords.remove(id) == this)

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
