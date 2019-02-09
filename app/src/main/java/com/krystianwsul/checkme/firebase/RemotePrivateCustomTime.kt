package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.records.RemoteCustomTimeRecord
import com.krystianwsul.checkme.firebase.records.RemotePrivateCustomTimeRecord
import com.krystianwsul.checkme.firebase.records.RemoteSharedCustomTimeRecord
import com.krystianwsul.checkme.utils.RemoteCustomTimeId

class RemotePrivateCustomTime(
        private val domainFactory: DomainFactory,
        override val remoteProject: RemotePrivateProject,
        override val remoteCustomTimeRecord: RemotePrivateCustomTimeRecord) : RemoteCustomTime<RemoteCustomTimeId.Private>() {

    override val id = remoteCustomTimeRecord.id

    val localId get() = remoteCustomTimeRecord.localId

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

    fun tryGetLocalCustomTime(domainFactory: DomainFactory) = remoteCustomTimeRecord
            .takeIf { it.mine(domainFactory) }
            ?.let {
                domainFactory.localFactory
                        .localCustomTimes
                        .singleOrNull { localCustomTime -> localCustomTime.id == it.localId }
            }

    // todo call after local version
    fun updateRemoteCustomTimeRecord(remoteSharedCustomTimeRecord: RemoteSharedCustomTimeRecord) {
        check(remoteSharedCustomTimeRecord.privateKey == id)

        remoteSharedCustomTimeRecord.name = remoteCustomTimeRecord.name

        remoteSharedCustomTimeRecord.sundayHour = remoteCustomTimeRecord.sundayHour
        remoteSharedCustomTimeRecord.sundayMinute = remoteCustomTimeRecord.sundayMinute

        remoteSharedCustomTimeRecord.mondayHour = remoteCustomTimeRecord.mondayHour
        remoteSharedCustomTimeRecord.mondayMinute = remoteCustomTimeRecord.mondayMinute

        remoteSharedCustomTimeRecord.tuesdayHour = remoteCustomTimeRecord.tuesdayHour
        remoteSharedCustomTimeRecord.tuesdayMinute = remoteCustomTimeRecord.tuesdayMinute

        remoteSharedCustomTimeRecord.wednesdayHour = remoteCustomTimeRecord.wednesdayHour
        remoteSharedCustomTimeRecord.wednesdayMinute = remoteCustomTimeRecord.wednesdayMinute

        remoteSharedCustomTimeRecord.thursdayHour = remoteCustomTimeRecord.thursdayHour
        remoteSharedCustomTimeRecord.thursdayMinute = remoteCustomTimeRecord.thursdayMinute

        remoteSharedCustomTimeRecord.fridayHour = remoteCustomTimeRecord.fridayHour
        remoteSharedCustomTimeRecord.fridayMinute = remoteCustomTimeRecord.fridayMinute

        remoteSharedCustomTimeRecord.saturdayHour = remoteCustomTimeRecord.saturdayHour
        remoteSharedCustomTimeRecord.saturdayMinute = remoteCustomTimeRecord.saturdayMinute
    }
}
