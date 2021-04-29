package com.krystianwsul.checkme.persistencemodel


import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.TimeDescriptor
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.TaskKeyData


class PersistenceManager(
        private val _instanceShownRecords: MutableList<InstanceShownRecord> = mutableListOf(),
        private var instanceShownMaxId: Int = 0,
        private val uuidRecord: UuidRecord = UuidRecord(true, UuidRecord.newUuid())
) {

    companion object {

        val instance by lazy {
            val sqLiteDatabase = MySQLiteHelper.database

            val instanceShownRecords = InstanceShownRecord.getInstancesShownRecords(sqLiteDatabase)
            val instanceShownMaxId = InstanceShownRecord.getMaxId(sqLiteDatabase)

            val uuidRecord = UuidRecord.getUuidRecord(sqLiteDatabase)

            PersistenceManager(instanceShownRecords, instanceShownMaxId, uuidRecord)
        }
    }

    val instanceShownRecords: MutableCollection<InstanceShownRecord>
        get() = _instanceShownRecords

    val uuid get() = uuidRecord.uuid

    fun save() = SaveService.Factory
            .instance
            .startService(this)

    fun createInstanceShownRecord(
            remoteTaskId: String,
            scheduleDate: Date,
            scheduleJsonTime: JsonTime,
            projectId: ProjectKey<*>,
    ): InstanceShownRecord {
        check(remoteTaskId.isNotEmpty())

        val id = ++instanceShownMaxId

        return InstanceShownRecord(
                false,
                id,
                remoteTaskId,
                scheduleDate.year,
                scheduleDate.month,
                scheduleDate.day,
                TimeDescriptor.fromJsonTime(scheduleJsonTime),
                mNotified = false,
                mNotificationShown = false,
                mProjectId = projectId.key,
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