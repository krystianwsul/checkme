package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.records.RemoteCustomTimeRecord
import com.krystianwsul.checkme.firebase.records.RemotePrivateCustomTimeRecord
import com.krystianwsul.checkme.utils.RemoteCustomTimeId

class RemotePrivateCustomTime(
        private val domainFactory: DomainFactory,
        override val remoteProject: RemotePrivateProject,
        override val remoteCustomTimeRecord: RemotePrivateCustomTimeRecord) : RemoteCustomTime<RemoteCustomTimeId.Private>() {

    override val id = remoteCustomTimeRecord.id

    private fun getSharedCustomTimes() = domainFactory.getSharedCustomTimes(id)

    override val allRecords
        get() = getSharedCustomTimes().map { it.remoteCustomTimeRecord }
                .toMutableList<RemoteCustomTimeRecord<*>>()
                .apply { add(remoteCustomTimeRecord) }

    var current
        get() = remoteCustomTimeRecord.current
        set(value) {
            remoteCustomTimeRecord.current = value
        }

    override fun delete() {
        remoteProject.deleteCustomTime(this)

        remoteCustomTimeRecord.delete()
    }
}
