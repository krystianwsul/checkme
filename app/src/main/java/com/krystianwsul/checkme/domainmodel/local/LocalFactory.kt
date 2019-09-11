package com.krystianwsul.checkme.domainmodel.local

import android.annotation.SuppressLint
import com.krystianwsul.checkme.domain.Instance
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord
import com.krystianwsul.checkme.persistencemodel.PersistenceManager
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.TaskKey

@SuppressLint("UseSparseArrays")
class LocalFactory(private val persistenceManager: PersistenceManager = PersistenceManager.instance) : Instance.ShownFactory {

    val instanceShownRecords: Collection<InstanceShownRecord>
        get() = persistenceManager.instanceShownRecords

    val uuid get() = persistenceManager.uuid

    private lateinit var domainFactory: DomainFactory

    fun initialize(domainFactory: DomainFactory) {
        this.domainFactory = domainFactory
    }

    fun save(source: SaveService.Source): Boolean = persistenceManager.save(source)

    override fun getShown(projectId: String, taskId: String, scheduleYear: Int, scheduleMonth: Int, scheduleDay: Int, scheduleCustomTimeId: RemoteCustomTimeId?, scheduleHour: Int?, scheduleMinute: Int?): InstanceShownRecord? {
        val matches: List<InstanceShownRecord>
        if (scheduleCustomTimeId != null) {
            check(scheduleHour == null)
            check(scheduleMinute == null)

            matches = persistenceManager.instanceShownRecords
                    .asSequence()
                    .filter { it.projectId == projectId }
                    .filter { it.taskId == taskId }
                    .filter { it.scheduleYear == scheduleYear }
                    .filter { it.scheduleMonth == scheduleMonth }
                    .filter { it.scheduleDay == scheduleDay }
                    .filter { it.scheduleCustomTimeId == scheduleCustomTimeId.value }
                    .toList()
        } else {
            checkNotNull(scheduleHour)
            checkNotNull(scheduleMinute)

            matches = persistenceManager.instanceShownRecords
                    .asSequence()
                    .filter { it.projectId == projectId }
                    .filter { it.taskId == taskId }
                    .filter { it.scheduleYear == scheduleYear }
                    .filter { it.scheduleMonth == scheduleMonth }
                    .filter { it.scheduleDay == scheduleDay }
                    .filter { it.scheduleHour == scheduleHour }
                    .filter { it.scheduleMinute == scheduleMinute }
                    .toList()
        }

        return matches.singleOrNull()
    }

    override fun createShown(remoteTaskId: String, scheduleDateTime: DateTime, projectId: String): InstanceShownRecord {
        val (remoteCustomTimeId, hour, minute) = scheduleDateTime.time
                .timePair
                .destructureRemote()

        return persistenceManager.createInstanceShownRecord(remoteTaskId, scheduleDateTime.date, remoteCustomTimeId, hour, minute, projectId)
    }

    fun deleteInstanceShownRecords(taskKeys: Set<TaskKey>) = persistenceManager.deleteInstanceShownRecords(taskKeys)
}
