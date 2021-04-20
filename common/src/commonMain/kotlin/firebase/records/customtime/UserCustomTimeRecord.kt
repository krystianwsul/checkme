package com.krystianwsul.common.firebase.records.customtime

import com.krystianwsul.common.firebase.json.customtimes.UserCustomTimeJson
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey


class UserCustomTimeRecord(
        create: Boolean,
        override val id: CustomTimeId.User,
        override val customTimeJson: UserCustomTimeJson,
        private val rootUserRecord: RootUserRecord,
) : CustomTimeRecord(create) {

    constructor(
            id: CustomTimeId.User,
            rootUserRecord: RootUserRecord,
            customTimeJson: UserCustomTimeJson,
    ) : this(false, id, customTimeJson, rootUserRecord)

    constructor(
            rootUserRecord: RootUserRecord,
            customTimeJson: UserCustomTimeJson,
    ) : this(true, rootUserRecord.newCustomTimeId(), customTimeJson, rootUserRecord)

    override val key get() = rootUserRecord.key + "/" + CUSTOM_TIMES + "/" + id

    override val customTimeKey = CustomTimeKey.User(rootUserRecord.userKey, id)

    override val createObject get() = customTimeJson

    var endTime by Committer(customTimeJson::endTime)

    val privateCustomTimeId by lazy {
        customTimeJson.privateCustomTimeId?.let { CustomTimeId.Project.Private(it) }
    }

    override fun deleteFromParent() = check(rootUserRecord.customTimeRecords.remove(id) == this)
}
