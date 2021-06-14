package com.krystianwsul.checkme.domainmodel.local

import android.annotation.SuppressLint
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord
import com.krystianwsul.checkme.persistencemodel.PersistenceManager
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.TimeDescriptor
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.TaskKeyData
import com.krystianwsul.common.utils.singleOrEmpty

@SuppressLint("UseSparseArrays")
class LocalFactory(
    private val persistenceManager: PersistenceManager = PersistenceManager.instance,
) : Instance.ShownFactory {

    val instanceShownRecords: Collection<InstanceShownRecord>
        get() = persistenceManager.instanceShownRecords

    fun save(): Boolean = persistenceManager.save()

    override fun getShown(taskKey: TaskKey, scheduleDateTime: DateTime): InstanceShownRecord? {
        val taskKeyData = TaskKeyData(taskKey)
        val scheduleDate = scheduleDateTime.date
        val scheduleTimeDescriptor = TimeDescriptor.fromJsonTime(JsonTime.fromTime(scheduleDateTime.time))

        return persistenceManager.instanceShownRecords
                .asSequence()
                .filter { it.taskKeyData == taskKeyData }
                .filter { it.scheduleYear == scheduleDate.year }
                .filter { it.scheduleMonth == scheduleDate.month }
                .filter { it.scheduleDay == scheduleDate.day }
                .filter { it.scheduleTimeDescriptor == scheduleTimeDescriptor }
                .toList()
                .singleOrEmpty()
    }

    override fun createShown(taskKeyData: TaskKeyData, scheduleDateTime: DateTime): InstanceShownRecord {
        return persistenceManager.createInstanceShownRecord(
                taskKeyData,
                scheduleDateTime.date,
                JsonTime.fromTime(scheduleDateTime.time),
        )
    }

    fun deleteInstanceShownRecords(taskKeys: Set<TaskKey>) = persistenceManager.deleteInstanceShownRecords(taskKeys)
}
