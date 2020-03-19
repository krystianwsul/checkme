package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.SharedCustomTimeRecord
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectType


class SharedCustomTime(
        override val project: SharedProject,
        override val customTimeRecord: SharedCustomTimeRecord
) : CustomTime<ProjectType.Shared>() {

    override val key = customTimeRecord.customTimeKey
    override val id = key.customTimeId as CustomTimeId.Shared

    val ownerKey get() = customTimeRecord.ownerKey
    val privateKey get() = customTimeRecord.privateKey

    override fun delete() {
        project.deleteCustomTime(this)

        customTimeRecord.delete()
    }
}