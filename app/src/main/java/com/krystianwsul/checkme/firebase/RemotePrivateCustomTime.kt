package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.records.RemoteCustomTimeRecord
import com.krystianwsul.checkme.firebase.records.RemotePrivateCustomTimeRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemotePrivateCustomTime(
        private val domainFactory: DomainFactory,
        override val remoteProject: RemotePrivateProject,
        override val remoteCustomTimeRecord: RemotePrivateCustomTimeRecord) : RemoteCustomTime<RemoteCustomTimeId.Private>() {

    override val id = remoteCustomTimeRecord.id

    override val customTimeKey by lazy { CustomTimeKey.Private(projectId, id) }

    private fun getSharedCustomTimes() = domainFactory.getSharedCustomTimes(id)

    override val allRecords
        get() = getSharedCustomTimes().map { it.remoteCustomTimeRecord }
                .toMutableList<RemoteCustomTimeRecord<*>>()
                .apply { add(remoteCustomTimeRecord) }

    fun current(exactTimeStamp: ExactTimeStamp): Boolean {
        val current = remoteCustomTimeRecord.current
        val endExactTimeStamp = endExactTimeStamp

        check(endExactTimeStamp == null || !current)

        return endExactTimeStamp?.let { it > exactTimeStamp } ?: current
    }

    var endExactTimeStamp
        get() = remoteCustomTimeRecord.endTime?.let { ExactTimeStamp(it) }
        set(value) {
            check((value == null) != (remoteCustomTimeRecord.endTime == null))

            remoteCustomTimeRecord.current = value == null
            remoteCustomTimeRecord.endTime = value?.long
        }

    override fun delete() {
        remoteProject.deleteCustomTime(this)

        remoteCustomTimeRecord.delete()
    }
}
