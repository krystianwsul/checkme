package com.krystianwsul.checkme.persistencemodel


import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.TaskKey


class PersistenceManager(
        private val _instanceShownRecords: MutableList<InstanceShownRecord> = mutableListOf(),
        private var instanceShownMaxId: Int = 0,
        private val uuidRecord: UuidRecord = UuidRecord(true, UuidRecord.newUuid())) {

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

    fun save(source: SaveService.Source) = SaveService.Factory
            .instance
            .startService(this, source)

    fun createInstanceShownRecord(remoteTaskId: String, scheduleDate: Date, remoteCustomTimeId: RemoteCustomTimeId?, hour: Int?, minute: Int?, projectId: String): InstanceShownRecord {
        check(remoteTaskId.isNotEmpty())
        check(projectId.isNotEmpty())

        val id = ++instanceShownMaxId

        return InstanceShownRecord(false, id, remoteTaskId, scheduleDate.year, scheduleDate.month, scheduleDate.day, remoteCustomTimeId?.value, hour, minute, false, false, projectId).also {
            _instanceShownRecords.add(it)
        }
    }

    fun deleteInstanceShownRecords(taskKeys: Set<TaskKey>) {
        val remove = _instanceShownRecords.filterNot { instanceShownRecord -> taskKeys.any { taskKey -> instanceShownRecord.projectId == taskKey.remoteProjectId && instanceShownRecord.taskId == taskKey.remoteTaskId } }

        remove.forEach { it.delete() }
    }
}