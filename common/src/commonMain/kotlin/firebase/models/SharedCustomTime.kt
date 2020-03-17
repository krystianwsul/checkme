package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.SharedCustomTimeRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId


class SharedCustomTime(
        override val project: SharedProject,
        override val customTimeRecord: SharedCustomTimeRecord
) : CustomTime<RemoteCustomTimeId.Shared, ProjectKey.Shared>() {

    override val key = customTimeRecord.customTimeKey
    override val id = key.remoteCustomTimeId

    val ownerKey get() = customTimeRecord.ownerKey
    val privateKey get() = customTimeRecord.privateKey

    override fun delete() {
        project.deleteCustomTime(this)

        customTimeRecord.delete()
    }
}