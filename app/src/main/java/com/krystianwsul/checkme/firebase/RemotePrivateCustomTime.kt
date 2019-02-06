package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.firebase.records.RemotePrivateCustomTimeRecord

class RemotePrivateCustomTime(
        override val remoteProject: RemotePrivateProject,
        override val remoteCustomTimeRecord: RemotePrivateCustomTimeRecord) : RemoteCustomTime() {

    override fun delete() {
        remoteProject.deleteCustomTime(this)

        remoteCustomTimeRecord.delete()
    }
}
