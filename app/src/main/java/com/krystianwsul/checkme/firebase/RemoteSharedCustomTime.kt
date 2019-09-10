package com.krystianwsul.checkme.firebase

import com.krystianwsul.common.firebase.records.RemoteSharedCustomTimeRecord
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.RemoteCustomTimeId


class RemoteSharedCustomTime(
        override val remoteProject: RemoteSharedProject,
        override val remoteCustomTimeRecord: RemoteSharedCustomTimeRecord) : RemoteCustomTime<RemoteCustomTimeId.Shared>() {

    override val id = remoteCustomTimeRecord.id

    override val customTimeKey by lazy { CustomTimeKey.Shared(projectId, id) }

    val ownerKey get() = remoteCustomTimeRecord.ownerKey
    val privateKey get() = remoteCustomTimeRecord.privateKey

    override val allRecords = listOf(remoteCustomTimeRecord)

    override fun delete() {
        remoteProject.deleteCustomTime(this)

        remoteCustomTimeRecord.delete()
    }
}