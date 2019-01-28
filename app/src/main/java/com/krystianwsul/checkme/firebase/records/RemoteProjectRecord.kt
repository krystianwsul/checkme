package com.krystianwsul.checkme.firebase.records

import android.text.TextUtils
import android.util.Log
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.json.*

@Suppress("LeakingThis")
abstract class RemoteProjectRecord(
        create: Boolean,
        domainFactory: DomainFactory,
        val id: String,
        private val projectJson: ProjectJson) : RemoteRecord(create) {

    companion object {

        const val PROJECT_JSON = "projectJson"
    }

    val remoteCustomTimeRecords = projectJson.customTimes
            .mapValues { (id, customTimeJson) ->
                check(!TextUtils.isEmpty(id))

                RemoteCustomTimeRecord(id, this, customTimeJson)
            }
            .toMutableMap()

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

    protected val createProjectJson
        get() = projectJson.apply {
            tasks = remoteTaskRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            taskHierarchies = remoteTaskHierarchyRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            customTimes = remoteCustomTimeRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            users = remoteUserRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()
        }

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

    override fun getValues(values: MutableMap<String, Any?>) {
        if (delete) {
            Log.e("asdf", "RemoteProjectRecord.getValues deleting " + this)

            check(update != null)

            values[key] = null
            delete = false
        } else {
            val children = remoteTaskRecords.values +
                    remoteTaskHierarchyRecords.values +
                    remoteCustomTimeRecords.values +
                    remoteUserRecords.values

            if (update == null) {
                Log.e("asdf", "RemoteProjectRecord.getValues creating " + this)

                values[key] = createObject
            } else {
                if (!update!!.isEmpty()) {
                    Log.e("asdf", "RemoteProjectRecord.getValues updating " + this)

                    values.putAll(update!!)
                }

                children.forEach { it.getValues(values) }
            }

            update = mutableMapOf()

            children.forEach { it.update = mutableMapOf() }
        }
    }

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

    fun newRemoteCustomTimeRecord(customTimeJson: CustomTimeJson): RemoteCustomTimeRecord {
        val remoteCustomTimeRecord = RemoteCustomTimeRecord(this, customTimeJson)
        check(!remoteCustomTimeRecords.containsKey(remoteCustomTimeRecord.id))

        remoteCustomTimeRecords[remoteCustomTimeRecord.id] = remoteCustomTimeRecord
        return remoteCustomTimeRecord
    }

    fun newRemoteUserRecord(userJson: UserJson): RemoteProjectUserRecord {
        val remoteProjectUserRecord = RemoteProjectUserRecord(true, this, userJson)
        check(!remoteCustomTimeRecords.containsKey(remoteProjectUserRecord.id))

        remoteUserRecords[remoteProjectUserRecord.id] = remoteProjectUserRecord
        return remoteProjectUserRecord
    }
}
