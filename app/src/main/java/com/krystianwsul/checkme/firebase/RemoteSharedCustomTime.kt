package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.firebase.records.RemoteSharedCustomTimeRecord

class RemoteSharedCustomTime(
        override val remoteProject: RemoteSharedProject,
        override val remoteCustomTimeRecord: RemoteSharedCustomTimeRecord) : RemoteCustomTime() {

    override fun delete() {
        remoteProject.deleteCustomTime(this)

        remoteCustomTimeRecord.delete()
    }
}