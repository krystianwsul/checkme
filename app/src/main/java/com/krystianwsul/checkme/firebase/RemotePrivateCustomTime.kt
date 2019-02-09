package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.records.RemotePrivateCustomTimeRecord
import com.krystianwsul.checkme.utils.RemoteCustomTimeId

class RemotePrivateCustomTime(
        override val remoteProject: RemotePrivateProject,
        override val remoteCustomTimeRecord: RemotePrivateCustomTimeRecord) : RemoteCustomTime<RemoteCustomTimeId.Private>() {

    override val id = remoteCustomTimeRecord.id

    val localId get() = remoteCustomTimeRecord.localId

    var current
        get() = remoteCustomTimeRecord.current
        set(value) {
            remoteCustomTimeRecord.current = value
        }

    override fun delete() {
        remoteProject.deleteCustomTime(this)

        remoteCustomTimeRecord.delete()
    }

    fun tryGetLocalCustomTime(domainFactory: DomainFactory) = remoteCustomTimeRecord
            .takeIf { it.mine(domainFactory) }
            ?.let {
                domainFactory.localFactory
                        .localCustomTimes
                        .singleOrNull { localCustomTime -> localCustomTime.id == it.localId }
            }
}
