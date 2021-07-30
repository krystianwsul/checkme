package com.krystianwsul.common.firebase.records.customtime

import com.krystianwsul.common.firebase.json.customtimes.SharedCustomTimeJson
import com.krystianwsul.common.firebase.records.project.SharedProjectRecord
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.UserKey


class SharedCustomTimeRecord(
    override val id: CustomTimeId.Project.Shared,
    override val projectRecord: SharedProjectRecord,
    override val customTimeJson: SharedCustomTimeJson,
) : ProjectCustomTimeRecord<ProjectType.Shared>(false) {

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
