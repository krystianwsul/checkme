package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemotePrivateProjectRecord(
        create: Boolean,
        id: String,
        projectJson: PrivateProjectJson
) : RemoteProjectRecord<RemoteCustomTimeId.Private, PrivateProjectJson>(
        create,
        id,
        projectJson
) {

    override val remoteCustomTimeRecords = projectJson.customTimes
            .map { (id, customTimeJson) ->
                check(id.isNotEmpty())

                val remoteCustomTimeId = RemoteCustomTimeId.Private(id)

                remoteCustomTimeId to RemotePrivateCustomTimeRecord(remoteCustomTimeId, this, customTimeJson)
            }
            .toMap()
            .toMutableMap()

    constructor(id: String, projectJson: PrivateProjectJson) : this(
            false,
            id,
            projectJson)

    constructor(deviceInfoDbInfo: DeviceDbInfo, projectJson: PrivateProjectJson) : this(
            true,
            deviceInfoDbInfo.key,
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

    fun getCustomTimeRecordId() = RemoteCustomTimeId.Private(DatabaseWrapper.instance.getPrivateCustomTimeRecordId(id))

    override fun getTaskRecordId() = DatabaseWrapper.instance.getPrivateTaskRecordId(id)

    override fun getScheduleRecordId(taskId: String) = DatabaseWrapper.instance.getPrivateScheduleRecordId(id, taskId)

    override fun getTaskHierarchyRecordId() = DatabaseWrapper.instance.getPrivateTaskHierarchyRecordId(id)

    override fun getCustomTimeRecord(id: String) = remoteCustomTimeRecords.getValue(RemoteCustomTimeId.Private(id))

    override fun getRemoteCustomTimeId(id: String) = RemoteCustomTimeId.Private(id)

    override fun getRemoteCustomTimeKey(remoteCustomTimeId: RemoteCustomTimeId.Private) = CustomTimeKey.Private(id, remoteCustomTimeId)
}