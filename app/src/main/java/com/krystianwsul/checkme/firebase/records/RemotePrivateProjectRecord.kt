package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.domainmodel.DeviceInfo
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.common.firebase.PrivateProjectJson

class RemotePrivateProjectRecord(
        create: Boolean,
        domainFactory: DomainFactory,
        id: String,
        override val projectJson: PrivateProjectJson) : RemoteProjectRecord<RemoteCustomTimeId.Private>(create, id, domainFactory.uuid) {

    override val remoteCustomTimeRecords = projectJson.customTimes
            .map { (id, customTimeJson) ->
                check(id.isNotEmpty())

                val remoteCustomTimeId = RemoteCustomTimeId.Private(id)

                remoteCustomTimeId to RemotePrivateCustomTimeRecord(remoteCustomTimeId, this, customTimeJson)
            }
            .toMap()
            .toMutableMap()

    constructor(domainFactory: DomainFactory, id: String, projectJson: PrivateProjectJson) : this(
            false,
            domainFactory,
            id,
            projectJson)

    constructor(domainFactory: DomainFactory, deviceInfo: DeviceInfo, projectJson: PrivateProjectJson) : this(
            true,
            domainFactory,
            deviceInfo.key,
            projectJson)

    fun newRemoteCustomTimeRecord(customTimeJson: com.krystianwsul.common.firebase.PrivateCustomTimeJson): RemotePrivateCustomTimeRecord {
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