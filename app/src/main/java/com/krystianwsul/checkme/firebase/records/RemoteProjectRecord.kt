package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.common.firebase.ProjectJson
import com.krystianwsul.common.firebase.TaskHierarchyJson
import com.krystianwsul.common.firebase.TaskJson

@Suppress("LeakingThis")
abstract class RemoteProjectRecord<T : RemoteCustomTimeId>(
        create: Boolean,
        domainFactory: DomainFactory,
        val id: String) : RemoteRecord(create) {

    companion object {

        const val PROJECT_JSON = "projectJson"
    }

    protected abstract val projectJson: ProjectJson

    abstract val remoteCustomTimeRecords: Map<out T, RemoteCustomTimeRecord<T>>

    val remoteTaskRecords by lazy {
        projectJson.tasks
                .mapValues { (id, taskJson) ->
                    check(id.isNotEmpty())

                    RemoteTaskRecord(id, domainFactory.uuid, this, taskJson)
                }
                .toMutableMap()
    }

    val remoteTaskHierarchyRecords by lazy {
        projectJson.taskHierarchies
                .mapValues { (id, taskHierarchyJson) ->
                    check(id.isNotEmpty())

                    RemoteTaskHierarchyRecord(id, this, taskHierarchyJson)
                }
                .toMutableMap()
    }

    override val key get() = id

    abstract val childKey: String

    var name: String
        get() = projectJson.name
        set(name) {
            check(name.isNotEmpty())

            if (name == projectJson.name)
                return

            projectJson.name = name
            addValue("$id/$PROJECT_JSON/name", name)
        }

    val startTime get() = projectJson.startTime

    var endTime
        get() = projectJson.endTime
        set(value) {
            if (value == projectJson.endTime)
                return

            projectJson.endTime = value
            addValue("$id/$PROJECT_JSON/endTime", value)
        }

    override val children
        get() = remoteTaskRecords.values +
                remoteTaskHierarchyRecords.values +
                remoteCustomTimeRecords.values

    fun newRemoteTaskRecord(domainFactory: DomainFactory, taskJson: TaskJson): RemoteTaskRecord<T> {
        val remoteTaskRecord = RemoteTaskRecord(domainFactory.uuid, this, taskJson)
        check(!remoteTaskRecords.containsKey(remoteTaskRecord.id))

        remoteTaskRecords[remoteTaskRecord.id] = remoteTaskRecord
        return remoteTaskRecord
    }

    fun newRemoteTaskHierarchyRecord(taskHierarchyJson: TaskHierarchyJson): RemoteTaskHierarchyRecord {
        val remoteTaskHierarchyRecord = RemoteTaskHierarchyRecord(this, taskHierarchyJson)
        check(!remoteTaskHierarchyRecords.containsKey(remoteTaskHierarchyRecord.id))

        remoteTaskHierarchyRecords[remoteTaskHierarchyRecord.id] = remoteTaskHierarchyRecord
        return remoteTaskHierarchyRecord
    }

    abstract fun getTaskHierarchyRecordId(): String

    abstract fun getTaskRecordId(): String

    abstract fun getScheduleRecordId(taskId: String): String

    abstract fun getCustomTimeRecord(id: String): RemoteCustomTimeRecord<T>

    abstract fun getRemoteCustomTimeId(id: String): T

    abstract fun getRemoteCustomTimeKey(projectId: String, customTimeId: String): CustomTimeKey<T>
}
