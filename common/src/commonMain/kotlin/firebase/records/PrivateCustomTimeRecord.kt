package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType


class PrivateCustomTimeRecord(
        create: Boolean,
        override val id: CustomTimeId.Project.Private,
        override val customTimeJson: PrivateCustomTimeJson,
        override val projectRecord: PrivateProjectRecord,
) : ProjectCustomTimeRecord<ProjectType.Private>(create) {

    constructor(
            id: CustomTimeId.Project.Private,
            remoteProjectRecord: PrivateProjectRecord,
            customTimeJson: PrivateCustomTimeJson,
    ) : this(false, id, customTimeJson, remoteProjectRecord)

    constructor(
            remoteProjectRecord: PrivateProjectRecord,
            customTimeJson: PrivateCustomTimeJson,
    ) : this(true, remoteProjectRecord.getCustomTimeRecordId(), customTimeJson, remoteProjectRecord)

    override val customTimeKey = CustomTimeKey.Project.Private(projectRecord.projectKey, id)

    override val createObject get() = customTimeJson

    override fun deleteFromParent() = check(projectRecord.customTimeRecords.remove(id) == this)

    var current by Committer(customTimeJson::current)
    var endTime by Committer(customTimeJson::endTime)
}
