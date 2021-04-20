package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.SharedCustomTimeJson
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.UserKey


class SharedCustomTimeRecord(
        create: Boolean,
        override val id: CustomTimeId.Project.Shared,
        override val customTimeJson: SharedCustomTimeJson,
        override val projectRecord: SharedProjectRecord,
) : ProjectCustomTimeRecord<ProjectType.Shared>(create) {

    constructor(
            id: CustomTimeId.Project.Shared,
            remoteProjectRecord: SharedProjectRecord,
            customTimeJson: SharedCustomTimeJson,
    ) : this(false, id, customTimeJson, remoteProjectRecord)

    constructor(
            remoteProjectRecord: SharedProjectRecord,
            customTimeJson: SharedCustomTimeJson,
    ) : this(true, remoteProjectRecord.getCustomTimeRecordId(), customTimeJson, remoteProjectRecord)

    override val customTimeKey = CustomTimeKey.Project.Shared(projectRecord.projectKey, id)

    override val createObject get() = customTimeJson

    override fun deleteFromParent() = check(projectRecord.customTimeRecords.remove(id) == this)

    val ownerKey by lazy { customTimeJson.ownerKey?.let(::UserKey) }

    val privateKey by lazy {
        customTimeJson.privateKey
                .takeUnless { it.isNullOrEmpty() }
                ?.let { CustomTimeId.Project.Private(it) }
    }
}
