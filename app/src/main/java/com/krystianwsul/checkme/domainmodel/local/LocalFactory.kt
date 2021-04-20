package com.krystianwsul.checkme.domainmodel.local

import android.annotation.SuppressLint
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord
import com.krystianwsul.checkme.persistencemodel.PersistenceManager
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.TimeDescriptor
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.singleOrEmpty

@SuppressLint("UseSparseArrays")
class LocalFactory(
        private val persistenceManager: PersistenceManager = PersistenceManager.instance,
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
            scheduleJsonTime: JsonTime,
    ): InstanceShownRecord? {
        val scheduleTimeDescriptor = TimeDescriptor.fromJsonTime(scheduleJsonTime)

        return persistenceManager.instanceShownRecords
                .asSequence()
                .filter { it.projectId == projectId.key }
                .filter { it.taskId == taskId }
                .filter { it.scheduleYear == scheduleYear }
                .filter { it.scheduleMonth == scheduleMonth }
                .filter { it.scheduleDay == scheduleDay }
                .filter { it.scheduleTimeDescriptor == scheduleTimeDescriptor }
                .toList()
                .singleOrEmpty()
    }

    override fun createShown(
            remoteTaskId: String,
            scheduleDateTime: DateTime,
            projectId: ProjectKey<*>,
    ): InstanceShownRecord {
        return persistenceManager.createInstanceShownRecord(
                remoteTaskId,
                scheduleDateTime.date,
                JsonTime.fromTime(scheduleDateTime.time),
                projectId,
        )
    }

    fun deleteInstanceShownRecords(taskKeys: Set<TaskKey>) = persistenceManager.deleteInstanceShownRecords(taskKeys)
}
