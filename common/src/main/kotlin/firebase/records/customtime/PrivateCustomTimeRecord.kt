package com.krystianwsul.common.firebase.records.customtime

import com.krystianwsul.common.firebase.json.customtimes.PrivateCustomTimeJson
import com.krystianwsul.common.firebase.records.project.PrivateProjectRecord
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType


class PrivateCustomTimeRecord(
    override val id: CustomTimeId.Project.Private,
    override val projectRecord: PrivateProjectRecord,
    override val customTimeJson: PrivateCustomTimeJson,
) : ProjectCustomTimeRecord<ProjectType.Private>(false) {

    override val customTimeKey = CustomTimeKey.Project.Private(projectRecord.projectKey, id)

    override val createObject get() = customTimeJson

    override fun deleteFromParent() = check(projectRecord.customTimeRecords.remove(id) == this)

    var current by Committer(customTimeJson::current)
    var endTime by Committer(customTimeJson::endTime)
}
