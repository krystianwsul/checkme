package com.krystianwsul.common.firebase.records.project

import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.json.projects.PrivateOwnedProjectJson
import com.krystianwsul.common.firebase.records.customtime.PrivateCustomTimeRecord
import com.krystianwsul.common.firebase.records.task.PrivateTaskRecord
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

class PrivateOwnedProjectRecord(
    create: Boolean,
    override val projectKey: ProjectKey.Private,
    private val projectJson: PrivateOwnedProjectJson,
) : OwnedProjectRecord<ProjectType.Private>(
    create,
    projectJson,
    projectKey,
    projectKey.key,
), PrivateProjectRecord {

    override val taskRecords = projectJson.tasks
        .mapValues { (id, taskJson) ->
            check(id.isNotEmpty())

            PrivateTaskRecord(id, this, taskJson)
        }
        .toMutableMap()

    override val customTimeRecords: MutableMap<CustomTimeId.Project.Private, PrivateCustomTimeRecord>

    init {
        initTaskHierarchyRecords()

        customTimeRecords = projectJson.customTimes
            .entries
            .associate { (id, customTimeJson) ->
                check(id.isNotEmpty())

                val customTimeId = CustomTimeId.Project.Private(id)

                customTimeId to PrivateCustomTimeRecord(customTimeId, this, customTimeJson)
            }
            .toMutableMap()
    }

    constructor(id: ProjectKey.Private, projectJson: PrivateOwnedProjectJson) : this(false, id, projectJson)

    constructor(userInfo: UserInfo, projectJson: PrivateOwnedProjectJson) :
            this(true, userInfo.key.toPrivateProjectKey(), projectJson)

    private val createProjectJson
        get() = projectJson.apply {
            tasks = taskRecords.values
                .associateBy({ it.id }, { it.createObject })
                .toMutableMap()

            taskHierarchies = taskHierarchyRecords.values
                .associateBy({ it.id.value }, { it.createObject })
                .toMutableMap()

            customTimes = customTimeRecords.values
                .associateBy({ it.id.value }, { it.createObject })
                .toMutableMap()
        }

    override val createObject get() = createProjectJson

    override val childKey get() = key

    var defaultTimesCreated by Committer(projectJson::defaultTimesCreated)

    override var ownerName by Committer(projectJson::ownerName)

    override fun deleteFromParent() = throw UnsupportedOperationException()

    override fun getCustomTimeRecord(id: String) =
        customTimeRecords.getValue(CustomTimeId.Project.Private(id))

    override fun getProjectCustomTimeId(id: String) = CustomTimeId.Project.Private(id)

    override fun getProjectCustomTimeKey(projectCustomTimeId: CustomTimeId.Project) =
        CustomTimeKey.Project.Private(projectKey, projectCustomTimeId as CustomTimeId.Project.Private)

    override fun deleteTaskRecord(taskRecord: TaskRecord) {
        check(taskRecords.remove(taskRecord.id) == taskRecord)
    }
}