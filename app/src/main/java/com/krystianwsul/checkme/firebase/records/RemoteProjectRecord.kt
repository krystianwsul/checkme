package com.krystianwsul.checkme.firebase.records

import android.text.TextUtils
import android.util.Log
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.json.*
import java.util.*

class RemoteProjectRecord(
        create: Boolean,
        domainFactory: DomainFactory,
        val id: String,
        private val jsonWrapper: JsonWrapper) : RemoteRecord(create) {

    companion object {

        const val PROJECT_JSON = "projectJson"
    }

    val remoteTaskRecords = HashMap<String, RemoteTaskRecord>()

    val remoteTaskHierarchyRecords = HashMap<String, RemoteTaskHierarchyRecord>()

    val remoteCustomTimeRecords = HashMap<String, RemoteCustomTimeRecord>()

    val remoteUserRecords = HashMap<String, RemoteProjectUserRecord>()

    override val key get() = id

    override val createObject: JsonWrapper
        get() {
            val projectJson = jsonWrapper.projectJson

            projectJson.tasks = remoteTaskRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            projectJson.taskHierarchies = remoteTaskHierarchyRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            projectJson.customTimes = remoteCustomTimeRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            projectJson.users = remoteUserRecords.values
                    .associateBy({ it.id }, { it.createObject })
                    .toMutableMap()

            return jsonWrapper
        }

    private val projectJson get() = jsonWrapper.projectJson

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

    constructor(domainFactory: DomainFactory, id: String, jsonWrapper: JsonWrapper) : this(
            false,
            domainFactory,
            id,
            jsonWrapper)

    constructor(domainFactory: DomainFactory, jsonWrapper: JsonWrapper) : this(
            true,
            domainFactory,
            DatabaseWrapper.getRootRecordId(),
            jsonWrapper)

    init {
        for ((id, taskJson) in projectJson.tasks) {
            check(!TextUtils.isEmpty(id))

            remoteTaskRecords[id] = RemoteTaskRecord(domainFactory, id, this, taskJson)
        }

        for ((id, taskHierarchyJson) in projectJson.taskHierarchies) {
            check(!TextUtils.isEmpty(id))

            remoteTaskHierarchyRecords[id] = RemoteTaskHierarchyRecord(id, this, taskHierarchyJson)
        }

        for ((id, customTimeJson) in projectJson.customTimes) {
            check(!TextUtils.isEmpty(id))

            remoteCustomTimeRecords[id] = RemoteCustomTimeRecord(id, this, customTimeJson)
        }

        for ((id, userJson) in projectJson.users) {
            check(!TextUtils.isEmpty(id))

            remoteUserRecords[id] = RemoteProjectUserRecord(false, this, userJson)
        }
    }

    fun updateRecordOf(addedFriends: Set<String>, removedFriends: Set<String>) {
        check(addedFriends.none { removedFriends.contains(it) })

        jsonWrapper.updateRecordOf(addedFriends, removedFriends)

        for (addedFriend in addedFriends) {
            addValue("$id/recordOf/$addedFriend", true)
        }

        for (removedFriend in removedFriends) {
            addValue("$id/recordOf/$removedFriend", null)
        }
    }

    override fun getValues(values: MutableMap<String, Any?>) {
        if (delete) {
            Log.e("asdf", "RemoteProjectRecord.getValues deleting " + this)

            check(update != null)

            values[key] = null
            delete = false
        } else {
            if (update == null) {
                Log.e("asdf", "RemoteProjectRecord.getValues creating " + this)

                check(update == null)

                values[key] = createObject
            } else {
                if (!update!!.isEmpty()) {
                    Log.e("asdf", "RemoteProjectRecord.getValues updating " + this)

                    values.putAll(update!!)
                }

                for (remoteTaskRecord in remoteTaskRecords.values)
                    remoteTaskRecord.getValues(values)

                for (remoteTaskHierarchyRecord in remoteTaskHierarchyRecords.values)
                    remoteTaskHierarchyRecord.getValues(values)

                for (remoteCustomTimeRecord in remoteCustomTimeRecords.values)
                    remoteCustomTimeRecord.getValues(values)

                for (remoteProjectUserRecord in remoteUserRecords.values)
                    remoteProjectUserRecord.getValues(values)
            }

            update = mutableMapOf()
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
