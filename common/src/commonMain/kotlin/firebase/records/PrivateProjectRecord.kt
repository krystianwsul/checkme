package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

class PrivateProjectRecord(
    private val databaseWrapper: DatabaseWrapper,
    create: Boolean,
    override val projectKey: ProjectKey.Private,
    private val projectJson: PrivateProjectJson
) : ProjectRecord<ProjectType.Private>(
    create,
    projectJson,
    projectKey
) {

    override lateinit var customTimeRecords: MutableMap<CustomTimeId.Private, PrivateCustomTimeRecord>
        private set

    init {
        initChildRecords(create)
    }

    override fun initChildRecords(create: Boolean) {
        super.initChildRecords(create)

        customTimeRecords = projectJson.customTimes
            .entries
            .associate { (id, customTimeJson) ->
                check(id.isNotEmpty())

                val customTimeId = CustomTimeId.Private(id)

                customTimeId to PrivateCustomTimeRecord(customTimeId, this, customTimeJson)
            }
            .toMutableMap()
    }

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
            tasks = taskRecords.values
                .associateBy({ it.id }, { it.createObject })
                .toMutableMap()

            taskHierarchies = taskHierarchyRecords.values
                .associateBy({ it.id }, { it.createObject })
                .toMutableMap()

            customTimes = customTimeRecords.values
                .associateBy({ it.id.value }, { it.createObject })
                .toMutableMap()
        }

    override val createObject get() = createProjectJson

    override val childKey get() = key

    override fun deleteFromParent() = throw UnsupportedOperationException()

    fun getCustomTimeRecordId() =
        CustomTimeId.Private(databaseWrapper.getPrivateCustomTimeRecordId(projectKey))

    override fun getTaskRecordId() = databaseWrapper.getPrivateTaskRecordId(projectKey)

    override fun getScheduleRecordId(taskId: String) =
        databaseWrapper.getPrivateScheduleRecordId(projectKey, taskId)

    override fun getTaskHierarchyRecordId() =
        databaseWrapper.getPrivateTaskHierarchyRecordId(projectKey)

    override fun getCustomTimeRecord(id: String) =
        customTimeRecords.getValue(CustomTimeId.Private(id))

    override fun getCustomTimeId(id: String) = CustomTimeId.Private(id)

    override fun newNoScheduleOrParentRecordId(taskId: String) =
        databaseWrapper.newPrivateNoScheduleOrParentRecordId(projectKey, taskId)

    override fun getCustomTimeKey(customTimeId: CustomTimeId<ProjectType.Private>) =
        CustomTimeKey.Private(projectKey, customTimeId as CustomTimeId.Private)
}