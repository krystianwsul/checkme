package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey


class UserCustomTimeRecord(
        create: Boolean,
        override val id: CustomTimeId.User,
        override val customTimeJson: PrivateCustomTimeJson,
        private val rootUserRecord: RootUserRecord,
) : CustomTimeRecord(create) {

    constructor(
            id: CustomTimeId.User,
            rootUserRecord: RootUserRecord,
            customTimeJson: PrivateCustomTimeJson,
    ) : this(false, id, customTimeJson, rootUserRecord)

    constructor(
            rootUserRecord: RootUserRecord,
            customTimeJson: PrivateCustomTimeJson,
    ) : this(true, rootUserRecord.newCustomTimeId(), customTimeJson, rootUserRecord)

    override val key get() = rootUserRecord.key + "/" + CUSTOM_TIMES + "/" + id

    override val customTimeKey = CustomTimeKey.User(rootUserRecord.userKey, id)

    override val createObject get() = customTimeJson

    var current by Committer(customTimeJson::current)
    var endTime by Committer(customTimeJson::endTime)

    override fun deleteFromParent() = check(rootUserRecord.customTimeRecords.remove(id) == this)
}
