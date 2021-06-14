package com.krystianwsul.checkme.persistencemodel


import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.TimeDescriptor
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.TaskKeyData


class PersistenceManager(
        private val _instanceShownRecords: MutableList<InstanceShownRecord> = mutableListOf(),
        private var instanceShownMaxId: Int = 0,
) {

    companion object {

        val instance by lazy {
            val sqLiteDatabase = MySQLiteHelper.database

            val instanceShownRecords = InstanceShownRecord.getInstancesShownRecords(sqLiteDatabase)
            val instanceShownMaxId = InstanceShownRecord.getMaxId(sqLiteDatabase)

            PersistenceManager(instanceShownRecords, instanceShownMaxId)
        }
    }

    val instanceShownRecords: MutableCollection<InstanceShownRecord>
        get() = _instanceShownRecords

    fun save() = SaveService.Factory
            .instance
            .startService(this)

    fun createInstanceShownRecord(
            taskKeyData: TaskKeyData,
            scheduleDate: Date,
            scheduleJsonTime: JsonTime,
    ): InstanceShownRecord {
        val id = ++instanceShownMaxId

        return InstanceShownRecord(
                false,
                id,
                taskKeyData.taskId,
                scheduleDate.year,
                scheduleDate.month,
                scheduleDate.day,
                TimeDescriptor.fromJsonTime(scheduleJsonTime),
                mNotified = false,
                mNotificationShown = false,
                mProjectId = taskKeyData.projectId,
        ).also { _instanceShownRecords.add(it) }
    }

    fun deleteInstanceShownRecords(taskKeys: Set<TaskKey>) {
        val taskKeyDatas = taskKeys.map(::TaskKeyData)

        val remove = _instanceShownRecords.filterNot {
            taskKeyDatas.any { taskKeyData -> it.projectId == taskKeyData.projectId && it.taskId == taskKeyData.taskId }
        }

        remove.forEach { it.delete() }
    }
}