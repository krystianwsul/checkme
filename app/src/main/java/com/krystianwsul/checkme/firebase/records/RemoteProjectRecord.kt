package com.krystianwsul.checkme.firebase.records

import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.json.ProjectJson
import com.krystianwsul.checkme.firebase.json.TaskHierarchyJson
import com.krystianwsul.checkme.firebase.json.TaskJson
import com.krystianwsul.checkme.firebase.json.UserJson

@Suppress("LeakingThis")
abstract class RemoteProjectRecord(
        create: Boolean,
        domainFactory: DomainFactory,
        val id: String) : RemoteRecord(create) {

    companion object {

        const val PROJECT_JSON = "projectJson"
    }

    protected abstract val projectJson: ProjectJson

    abstract val remoteCustomTimeRecords: Map<String, RemoteCustomTimeRecord>

    val remoteTaskRecords = projectJson.tasks
            .mapValues { (id, taskJson) ->
                check(!TextUtils.isEmpty(id))

                RemoteTaskRecord(domainFactory, id, this, taskJson)
            }
            .toMutableMap()

    val remoteTaskHierarchyRecords = projectJson.taskHierarchies
            .mapValues { (id, taskHierarchyJson) ->
                check(!TextUtils.isEmpty(id))

                RemoteTaskHierarchyRecord(id, this, taskHierarchyJson)
            }
            .toMutableMap()

    val remoteUserRecords = projectJson.users
            .mapValues { (id, userJson) ->
                check(!TextUtils.isEmpty(id))

                RemoteProjectUserRecord(false, this, userJson)
            }
            .toMutableMap()

    override val key get() = id

    abstract val childKey: String

    var name: String
        get() = projectJson.name
        set(name) {
            check(!TextUtils.isEmpty(name))

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
                remoteCustomTimeRecords.values +
                remoteUserRecords.values

    fun newRemoteTaskRecord(domainFactory: DomainFactory, taskJson: TaskJson): RemoteTaskRecord {
        val remoteTaskRecord = RemoteTaskRecord(domainFactory, this, taskJson)
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

    fun newRemoteUserRecord(userJson: UserJson): RemoteProjectUserRecord {
        val remoteProjectUserRecord = RemoteProjectUserRecord(true, this, userJson)
        check(!remoteCustomTimeRecords.containsKey(remoteProjectUserRecord.id))

        remoteUserRecords[remoteProjectUserRecord.id] = remoteProjectUserRecord
        return remoteProjectUserRecord
    }
}
