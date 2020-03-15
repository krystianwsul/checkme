package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.RemoteSharedCustomTimeRecord
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId


class SharedCustomTime(
        override val project: SharedProject,
        override val remoteCustomTimeRecord: RemoteSharedCustomTimeRecord
) : CustomTime<RemoteCustomTimeId.Shared, ProjectKey.Shared>() {

    override val id = remoteCustomTimeRecord.id

    override val customTimeKey by lazy { CustomTimeKey.Shared(projectId, id) }

    val ownerKey get() = remoteCustomTimeRecord.ownerKey
    val privateKey get() = remoteCustomTimeRecord.privateKey

    override fun delete() {
        project.deleteCustomTime(this)

        remoteCustomTimeRecord.delete()
    }
}