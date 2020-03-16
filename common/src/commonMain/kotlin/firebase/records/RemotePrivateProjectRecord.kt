package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemotePrivateProjectRecord(
        private val databaseWrapper: DatabaseWrapper,
        create: Boolean,
        id: ProjectKey.Private,
        projectJson: PrivateProjectJson
) : RemoteProjectRecord<RemoteCustomTimeId.Private, PrivateProjectJson, ProjectKey.Private>(
        create,
        id,
        projectJson
) {

    override val customTimeRecords = projectJson.customTimes
            .map { (id, customTimeJson) ->
                check(id.isNotEmpty())

                val remoteCustomTimeId = RemoteCustomTimeId.Private(id)

                remoteCustomTimeId to PrivateCustomTimeRecord(remoteCustomTimeId, this, customTimeJson)
            }
            .toMap()
            .toMutableMap()

    constructor(
            databaseWrapper: DatabaseWrapper,
            id: ProjectKey.Private,
            projectJson: PrivateProjectJson
    ) : this(databaseWrapper, false, id, projectJson)

    constructor(
            databaseWrapper: DatabaseWrapper,
            userInfo: UserInfo,
            projectJson: PrivateProjectJson
    ) : this(databaseWrapper, true, userInfo.key.toPrivateProjectKey(), projectJson)

    fun newRemoteCustomTimeRecord(customTimeJson: PrivateCustomTimeJson): PrivateCustomTimeRecord {
        val remoteCustomTimeRecord = PrivateCustomTimeRecord(this, customTimeJson)
        check(!customTimeRecords.containsKey(remoteCustomTimeRecord.id))

        customTimeRecords[remoteCustomTimeRecord.id] = remoteCustomTimeRecord
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

            customTimes = customTimeRecords.values
                    .associateBy({ it.id.value }, { it.createObject })
                    .toMutableMap()
        }

    override val createObject get() = createProjectJson

    override val childKey get() = key

    override fun deleteFromParent() = throw UnsupportedOperationException()

    fun getCustomTimeRecordId() = RemoteCustomTimeId.Private(databaseWrapper.getPrivateCustomTimeRecordId(id))

    override fun getTaskRecordId() = databaseWrapper.getPrivateTaskRecordId(id)

    override fun getScheduleRecordId(taskId: String) = databaseWrapper.getPrivateScheduleRecordId(id, taskId)

    override fun getTaskHierarchyRecordId() = databaseWrapper.getPrivateTaskHierarchyRecordId(id)

    override fun getCustomTimeRecord(id: String) = customTimeRecords.getValue(RemoteCustomTimeId.Private(id))

    override fun getRemoteCustomTimeId(id: String) = RemoteCustomTimeId.Private(id)

    override fun getRemoteCustomTimeKey(remoteCustomTimeId: RemoteCustomTimeId.Private) = CustomTimeKey.Private(id, remoteCustomTimeId)
}