package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemotePrivateProjectRecord(
        create: Boolean,
        id: String,
        uuid: String,
        override val projectJson: PrivateProjectJson
) : RemoteProjectRecord<RemoteCustomTimeId.Private>(
        create,
        id,
        uuid
) {

    override val remoteCustomTimeRecords = projectJson.customTimes
            .map { (id, customTimeJson) ->
                check(id.isNotEmpty())

                val remoteCustomTimeId = RemoteCustomTimeId.Private(id)

                remoteCustomTimeId to RemotePrivateCustomTimeRecord(remoteCustomTimeId, this, customTimeJson)
            }
            .toMap()
            .toMutableMap()

    constructor(id: String, uuid: String, projectJson: PrivateProjectJson) : this(
            false,
            id,
            uuid,
            projectJson)

    constructor(deviceInfo: DeviceInfo, uuid: String, projectJson: PrivateProjectJson) : this(
            true,
            deviceInfo.key,
            uuid,
            projectJson)

    fun newRemoteCustomTimeRecord(customTimeJson: com.krystianwsul.common.firebase.json.PrivateCustomTimeJson): RemotePrivateCustomTimeRecord {
        val remoteCustomTimeRecord = RemotePrivateCustomTimeRecord(this, customTimeJson)
        check(!remoteCustomTimeRecords.containsKey(remoteCustomTimeRecord.id))

        remoteCustomTimeRecords[remoteCustomTimeRecord.id] = remoteCustomTimeRecord
        return remoteCustomTimeRecord
    }

    private val createProjectJson
        get() = projectJson.apply {
            tasks = remoteTaskRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            taskHierarchies = remoteTaskHierarchyRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            customTimes = remoteCustomTimeRecords.values
                    .associateBy({ it.id.value }, { it.createObject })
                    .toMutableMap()
        }

    override val createObject get() = createProjectJson

    override val childKey get() = key

    override fun deleteFromParent() = throw UnsupportedOperationException()

    fun getCustomTimeRecordId() = RemoteCustomTimeId.Private(DatabaseWrapper.getPrivateCustomTimeRecordId(id))

    override fun getTaskRecordId() = DatabaseWrapper.getPrivateTaskRecordId(id)

    override fun getScheduleRecordId(taskId: String) = DatabaseWrapper.getPrivateScheduleRecordId(id, taskId)

    override fun getTaskHierarchyRecordId() = DatabaseWrapper.getPrivateTaskHierarchyRecordId(id)

    override fun getCustomTimeRecord(id: String) = remoteCustomTimeRecords.getValue(RemoteCustomTimeId.Private(id))

    override fun getRemoteCustomTimeId(id: String) = RemoteCustomTimeId.Private(id)

    override fun getRemoteCustomTimeKey(projectId: String, customTimeId: String) = CustomTimeKey.Private(projectId, getRemoteCustomTimeId(customTimeId))
}