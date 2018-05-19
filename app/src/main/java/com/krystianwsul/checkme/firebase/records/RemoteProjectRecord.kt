package com.krystianwsul.checkme.firebase.records

import android.text.TextUtils
import android.util.Log
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.json.*
import junit.framework.Assert
import java.util.*

class RemoteProjectRecord : RemoteRecord {

    companion object {

        const val PROJECT_JSON = "projectJson"
    }

    val id: String

    private val jsonWrapper: JsonWrapper

    val remoteTaskRecords = HashMap<String, RemoteTaskRecord>()

    val remoteTaskHierarchyRecords = HashMap<String, RemoteTaskHierarchyRecord>()

    val remoteCustomTimeRecords = HashMap<String, RemoteCustomTimeRecord>()

    val remoteUserRecords = HashMap<String, RemoteProjectUserRecord>()

    public override val key get() = id

    override val createObject: JsonWrapper
        get() {
            val projectJson = jsonWrapper.projectJson!!

            projectJson.tasks = remoteTaskRecords.values.associateBy({ it.id }, { it.createObject })

            projectJson.taskHierarchies = remoteTaskHierarchyRecords.values.associateBy({ it.id }, { it.createObject })

            projectJson.customTimes = remoteCustomTimeRecords.values.associateBy({ it.id }, { it.createObject })

            projectJson.users = remoteUserRecords.values.associateBy({ it.id }, { it.createObject })

            return jsonWrapper
        }

    private val projectJson get() = jsonWrapper.projectJson!!

    var name: String
        get() = projectJson.name
        set(name) {
            Assert.assertTrue(!TextUtils.isEmpty(name))

            if (name == projectJson.name)
                return

            projectJson.name = name
            addValue("$id/$PROJECT_JSON/name", name)
        }

    val startTime get() = projectJson.startTime

    val endTime get() = projectJson.endTime

    constructor(domainFactory: DomainFactory, id: String, jsonWrapper: JsonWrapper) : super(false) {
        this.id = id
        this.jsonWrapper = jsonWrapper

        initialize(domainFactory)
    }

    constructor(domainFactory: DomainFactory, jsonWrapper: JsonWrapper) : super(true) {
        id = DatabaseWrapper.getRootRecordId()
        this.jsonWrapper = jsonWrapper

        initialize(domainFactory)
    }

    private fun initialize(domainFactory: DomainFactory) {
        for ((id, taskJson) in projectJson.tasks) {
            Assert.assertTrue(!TextUtils.isEmpty(id))

            Assert.assertTrue(taskJson != null)

            remoteTaskRecords[id] = RemoteTaskRecord(domainFactory, id, this, taskJson!!)
        }

        for ((id, taskHierarchyJson) in projectJson.taskHierarchies) {
            Assert.assertTrue(taskHierarchyJson != null)

            Assert.assertTrue(!TextUtils.isEmpty(id))

            remoteTaskHierarchyRecords[id] = RemoteTaskHierarchyRecord(id, this, taskHierarchyJson!!)
        }

        for ((id, customTimeJson) in projectJson.customTimes) {
            Assert.assertTrue(!TextUtils.isEmpty(id))

            Assert.assertTrue(customTimeJson != null)

            remoteCustomTimeRecords[id] = RemoteCustomTimeRecord(id, this, customTimeJson!!)
        }

        for ((id, userJson) in projectJson.users) {
            Assert.assertTrue(!TextUtils.isEmpty(id))

            Assert.assertTrue(userJson != null)

            remoteUserRecords[id] = RemoteProjectUserRecord(false, this, userJson!!)
        }
    }

    fun updateRecordOf(addedFriends: Set<String>, removedFriends: Set<String>) {
        Assert.assertTrue(addedFriends.none { removedFriends.contains(it) })

        jsonWrapper.updateRecordOf(addedFriends, removedFriends)

        for (addedFriend in addedFriends) {
            addValue("$id/recordOf/$addedFriend", true)
        }

        for (removedFriend in removedFriends) {
            addValue("$id/recordOf/$removedFriend", null)
        }
    }

    fun setEndTime(endTime: Long) {
        projectJson.setEndTime(endTime)
        addValue("$id/$PROJECT_JSON/endTime", endTime)
    }

    override fun getValues(values: MutableMap<String, Any?>) {
        Assert.assertTrue(!deleted)
        Assert.assertTrue(!created)
        Assert.assertTrue(!updated)

        when {
            delete -> {
                Log.e("asdf", "RemoteProjectRecord.getValues deleting " + this)

                Assert.assertTrue(!create)
                Assert.assertTrue(update != null)

                deleted = true
                values[key] = null
            }
            create -> {
                Log.e("asdf", "RemoteProjectRecord.getValues creating " + this)

                Assert.assertTrue(update == null)

                created = true

                values[key] = createObject
            }
            else -> {
                Assert.assertTrue(update != null)

                if (!update!!.isEmpty()) {
                    Log.e("asdf", "RemoteProjectRecord.getValues updating " + this)

                    updated = true
                    values.putAll(update)
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
        }
    }

    fun newRemoteTaskRecord(domainFactory: DomainFactory, taskJson: TaskJson): RemoteTaskRecord {
        val remoteTaskRecord = RemoteTaskRecord(domainFactory, this, taskJson)
        Assert.assertTrue(!remoteTaskRecords.containsKey(remoteTaskRecord.id))

        remoteTaskRecords[remoteTaskRecord.id] = remoteTaskRecord
        return remoteTaskRecord
    }

    fun newRemoteTaskHierarchyRecord(taskHierarchyJson: TaskHierarchyJson): RemoteTaskHierarchyRecord {
        val remoteTaskHierarchyRecord = RemoteTaskHierarchyRecord(this, taskHierarchyJson)
        Assert.assertTrue(!remoteTaskHierarchyRecords.containsKey(remoteTaskHierarchyRecord.id))

        remoteTaskHierarchyRecords[remoteTaskHierarchyRecord.id] = remoteTaskHierarchyRecord
        return remoteTaskHierarchyRecord
    }

    fun newRemoteCustomTimeRecord(customTimeJson: CustomTimeJson): RemoteCustomTimeRecord {
        val remoteCustomTimeRecord = RemoteCustomTimeRecord(this, customTimeJson)
        Assert.assertTrue(!remoteCustomTimeRecords.containsKey(remoteCustomTimeRecord.id))

        remoteCustomTimeRecords[remoteCustomTimeRecord.id] = remoteCustomTimeRecord
        return remoteCustomTimeRecord
    }

    fun newRemoteUserRecord(userJson: UserJson): RemoteProjectUserRecord {
        val remoteProjectUserRecord = RemoteProjectUserRecord(true, this, userJson)
        Assert.assertTrue(!remoteCustomTimeRecords.containsKey(remoteProjectUserRecord.id))

        remoteUserRecords[remoteProjectUserRecord.id] = remoteProjectUserRecord
        return remoteProjectUserRecord
    }
}
