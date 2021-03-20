package com.krystianwsul.checkme.domainmodel.local

import android.annotation.SuppressLint
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord
import com.krystianwsul.checkme.persistencemodel.PersistenceManager
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey

@SuppressLint("UseSparseArrays")
class LocalFactory(
        private val persistenceManager: PersistenceManager = PersistenceManager.instance
) : Instance.ShownFactory, FactoryProvider.Local {

    val instanceShownRecords: Collection<InstanceShownRecord>
        get() = persistenceManager.instanceShownRecords

    override val uuid get() = persistenceManager.uuid

    fun save(): Boolean = persistenceManager.save()

    override fun getShown(
            projectId: ProjectKey<*>,
            taskId: String,
            scheduleYear: Int,
            scheduleMonth: Int,
            scheduleDay: Int,
            scheduleCustomTimeId: CustomTimeId<*>?,
            scheduleHour: Int?,
            scheduleMinute: Int?
    ): InstanceShownRecord? {
        val matches: List<InstanceShownRecord>
        if (scheduleCustomTimeId != null) {
            check(scheduleHour == null)
            check(scheduleMinute == null)

            matches = persistenceManager.instanceShownRecords
                    .asSequence()
                    .filter { it.projectId == projectId.key }
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
                    .filter { it.projectId == projectId.key }
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

    override fun createShown(
            remoteTaskId: String,
            scheduleDateTime: DateTime,
            projectId: ProjectKey<*>
    ): InstanceShownRecord {
        val (customTimeId, hour, minute) = scheduleDateTime.time
                .timePair
                .destructureRemote()

        return persistenceManager.createInstanceShownRecord(
                remoteTaskId,
                scheduleDateTime.date,
                customTimeId,
                hour,
                minute,
                projectId
        )
    }

    fun deleteInstanceShownRecords(taskKeys: Set<TaskKey>) = persistenceManager.deleteInstanceShownRecords(taskKeys)
}
