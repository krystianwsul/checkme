package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.firebase.records.RemoteSharedCustomTimeRecord
import com.krystianwsul.checkme.utils.RemoteCustomTimeId

class RemoteSharedCustomTime(
        override val remoteProject: RemoteSharedProject,
        override val remoteCustomTimeRecord: RemoteSharedCustomTimeRecord) : RemoteCustomTime<RemoteCustomTimeId.Shared>() {

    override val id = remoteCustomTimeRecord.id

    val ownerKey get() = remoteCustomTimeRecord.ownerKey
    val privateKey get() = remoteCustomTimeRecord.privateKey

    override val allRecords = listOf(remoteCustomTimeRecord)

    override fun delete() {
        remoteProject.deleteCustomTime(this)

        remoteCustomTimeRecord.delete()
    }
}