package com.krystianwsul.checkme.domainmodel.local

import android.annotation.SuppressLint
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord
import com.krystianwsul.checkme.persistencemodel.PersistenceManager
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey

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
        val preMatches = persistenceManager.instanceShownRecords
                .asSequence()
                .filter { it.projectId == projectId.key }
                .filter { it.taskId == taskId }
                .filter { it.scheduleYear == scheduleYear }
                .filter { it.scheduleMonth == scheduleMonth }
                .filter { it.scheduleDay == scheduleDay }

        val matches = when (scheduleJsonTime) {
            is JsonTime.Custom -> {
                val json = scheduleJsonTime.toJson()

                preMatches.filter { it.scheduleCustomTimeId == json }
            }
            is JsonTime.Normal -> {
                val (hour, minute) = scheduleJsonTime.hourMinute
                preMatches.filter { it.scheduleHour == hour }.filter { it.scheduleMinute == minute }
            }
            else -> throw UnsupportedOperationException() // needed for compilation
        }

        return matches.singleOrNull()
    }

    override fun createShown(
            remoteTaskId: String,
            scheduleDateTime: DateTime,
            projectId: ProjectKey<*>,
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
