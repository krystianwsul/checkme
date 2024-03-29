package com.krystianwsul.common.firebase.models.customtime

import com.krystianwsul.common.firebase.models.project.SharedOwnedProject
import com.krystianwsul.common.firebase.records.customtime.SharedCustomTimeRecord
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectType


class SharedCustomTime(
    override val project: SharedOwnedProject,
    override val customTimeRecord: SharedCustomTimeRecord,
) : Time.Custom.Project<ProjectType.Shared>() {

    override val key = customTimeRecord.customTimeKey
    override val id = key.customTimeId as CustomTimeId.Project.Shared

    val ownerKey get() = customTimeRecord.ownerKey
    val privateKey get() = customTimeRecord.privateKey
}