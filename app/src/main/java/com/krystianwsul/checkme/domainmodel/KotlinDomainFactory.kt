package com.krystianwsul.checkme.domainmodel

import android.text.TextUtils
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.domainmodel.local.LocalInstance
import com.krystianwsul.checkme.firebase.RemoteInstance
import com.krystianwsul.checkme.firebase.RemoteProjectFactory
import com.krystianwsul.checkme.firebase.RemoteRootUser
import com.krystianwsul.checkme.persistencemodel.PersistenceManger
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import java.util.*

class KotlinDomainFactory(persistenceManager: PersistenceManger?) {

    companion object {

        var _kotlinDomainFactory: KotlinDomainFactory? = null

        @Synchronized
        fun getKotlinDomainFactory(persistenceManager: PersistenceManger? = null): KotlinDomainFactory {
            if (_kotlinDomainFactory == null)
                _kotlinDomainFactory = KotlinDomainFactory(persistenceManager)
            return _kotlinDomainFactory!!
        }
    }

    val domainFactory: DomainFactory

    private var start: ExactTimeStamp
    private var read: ExactTimeStamp
    private var stop: ExactTimeStamp

    val readMillis get() = read.long - start.long
    val instantiateMillis get() = stop.long - read.long

    var userInfo: UserInfo? = null

    var recordQuery: Query? = null
    var recordListener: ValueEventListener? = null

    var userQuery: Query? = null
    var userListener: ValueEventListener? = null

    @JvmField
    var localFactory: LocalFactory

    var remoteProjectFactory: RemoteProjectFactory? = null

    var remoteRootUser: RemoteRootUser? = null

    val notTickFirebaseListeners = mutableListOf<(DomainFactory) -> Unit>()

    var tickData: TickData? = null

    var skipSave = false

    val lastNotificationBeeps = mutableMapOf<InstanceKey, Long>()

    init {
        start = ExactTimeStamp.now

        domainFactory = DomainFactory(this)
        localFactory = persistenceManager?.let { LocalFactory(it) } ?: LocalFactory.instance

        read = ExactTimeStamp.now

        localFactory.initialize(this)

        stop = ExactTimeStamp.now
    }

    // internal

    private fun getExistingInstanceIfPresent(taskKey: TaskKey, scheduleDateTime: DateTime): Instance? {
        val instanceKey = InstanceKey(taskKey, scheduleDateTime.date, scheduleDateTime.time.timePair)

        return getExistingInstanceIfPresent(instanceKey)
    }

    private fun getExistingInstanceIfPresent(instanceKey: InstanceKey): Instance? {
        return if (instanceKey.taskKey.localTaskId != null) {
            check(TextUtils.isEmpty(instanceKey.taskKey.remoteProjectId))
            check(TextUtils.isEmpty(instanceKey.taskKey.remoteTaskId))

            localFactory.getExistingInstanceIfPresent(instanceKey)
        } else {
            check(!TextUtils.isEmpty(instanceKey.taskKey.remoteProjectId))
            check(!TextUtils.isEmpty(instanceKey.taskKey.remoteTaskId))
            checkNotNull(remoteProjectFactory)

            remoteProjectFactory!!.getExistingInstanceIfPresent(instanceKey)
        }
    }

    fun getRemoteCustomTimeId(projectId: String, customTimeKey: CustomTimeKey): String {
        if (!TextUtils.isEmpty(customTimeKey.remoteProjectId)) {
            check(!TextUtils.isEmpty(customTimeKey.remoteCustomTimeId))
            check(customTimeKey.localCustomTimeId == null)

            check(customTimeKey.remoteProjectId == projectId)

            return customTimeKey.remoteCustomTimeId!!
        } else {
            check(TextUtils.isEmpty(customTimeKey.remoteCustomTimeId))
            checkNotNull(customTimeKey.localCustomTimeId)

            val localCustomTime = localFactory.getLocalCustomTime(customTimeKey.localCustomTimeId!!)

            check(localCustomTime.hasRemoteRecord(projectId))

            return localCustomTime.getRemoteId(projectId)
        }
    }

    private fun generateInstance(taskKey: TaskKey, scheduleDateTime: DateTime): Instance {
        if (taskKey.localTaskId != null) {
            check(TextUtils.isEmpty(taskKey.remoteProjectId))
            check(TextUtils.isEmpty(taskKey.remoteTaskId))

            return LocalInstance(this, taskKey.localTaskId, scheduleDateTime)
        } else {
            check(remoteProjectFactory != null)
            check(!TextUtils.isEmpty(taskKey.remoteProjectId))
            check(!TextUtils.isEmpty(taskKey.remoteTaskId))

            val remoteCustomTimeId: String?
            val hour: Int?
            val minute: Int?

            val customTimeKey = scheduleDateTime.time.timePair.customTimeKey
            val hourMinute = scheduleDateTime.time.timePair.hourMinute

            if (customTimeKey != null) {
                check(hourMinute == null)

                remoteCustomTimeId = getRemoteCustomTimeId(taskKey.remoteProjectId!!, customTimeKey)

                hour = null
                minute = null
            } else {
                checkNotNull(hourMinute)

                remoteCustomTimeId = null

                hour = hourMinute!!.hour
                minute = hourMinute.minute
            }

            val instanceShownRecord = localFactory.getInstanceShownRecord(taskKey.remoteProjectId!!, taskKey.remoteTaskId!!, scheduleDateTime.date.year, scheduleDateTime.date.month, scheduleDateTime.date.day, remoteCustomTimeId, hour, minute)

            val remoteProject = remoteProjectFactory!!.getTaskForce(taskKey).remoteProject

            return RemoteInstance(this, remoteProject, taskKey.remoteTaskId, scheduleDateTime, instanceShownRecord)
        }
    }

    fun getInstance(taskKey: TaskKey, scheduleDateTime: DateTime): Instance {
        val existingInstance = getExistingInstanceIfPresent(taskKey, scheduleDateTime)

        return existingInstance ?: generateInstance(taskKey, scheduleDateTime)
    }

    fun getInstance(instanceKey: InstanceKey): Instance {
        getExistingInstanceIfPresent(instanceKey)?.let { return it }

        val dateTime = getDateTime(instanceKey.scheduleKey.scheduleDate, instanceKey.scheduleKey.scheduleTimePair)

        return generateInstance(instanceKey.taskKey, dateTime) // DateTime -> timePair
    }

    fun getPastInstances(task: Task, now: ExactTimeStamp): List<Instance> {
        val allInstances = HashMap<InstanceKey, Instance>()

        allInstances.putAll(task.existingInstances
                .values
                .filter { it.scheduleDateTime.timeStamp.toExactTimeStamp() <= now }
                .associateBy { it.instanceKey })

        allInstances.putAll(task.getInstances(null, now.plusOne(), now).associateBy { it.instanceKey })

        return ArrayList(allInstances.values)
    }

    fun getRootInstances(startExactTimeStamp: ExactTimeStamp?, endExactTimeStamp: ExactTimeStamp, now: ExactTimeStamp): List<Instance> {
        check(startExactTimeStamp == null || startExactTimeStamp < endExactTimeStamp)

        val allInstances = HashMap<InstanceKey, Instance>()

        for (instance in domainFactory.existingInstances) {
            val instanceExactTimeStamp = instance.instanceDateTime
                    .timeStamp
                    .toExactTimeStamp()

            if (startExactTimeStamp != null && startExactTimeStamp > instanceExactTimeStamp)
                continue

            if (endExactTimeStamp <= instanceExactTimeStamp)
                continue

            allInstances[instance.instanceKey] = instance
        }

        domainFactory.tasks.forEach { task ->
            for (instance in task.getInstances(startExactTimeStamp, endExactTimeStamp, now)) {
                val instanceExactTimeStamp = instance.instanceDateTime.timeStamp.toExactTimeStamp()

                if (startExactTimeStamp != null && startExactTimeStamp > instanceExactTimeStamp)
                    continue

                if (endExactTimeStamp <= instanceExactTimeStamp)
                    continue

                allInstances[instance.instanceKey] = instance
            }
        }

        return allInstances.values.filter { it.isRootInstance(now) && it.isVisible(now) }
    }

    fun getTime(timePair: TimePair) = if (timePair.hourMinute != null) {
        check(timePair.customTimeKey == null)

        NormalTime(timePair.hourMinute)
    } else {
        checkNotNull(timePair.customTimeKey)

        getCustomTime(timePair.customTimeKey!!)
    }

    private fun getDateTime(date: Date, timePair: TimePair) = DateTime(date, getTime(timePair))

    fun getParentTask(childTask: Task, exactTimeStamp: ExactTimeStamp): Task? {
        check(childTask.notDeleted(exactTimeStamp))

        val parentTaskHierarchy = domainFactory.getParentTaskHierarchy(childTask, exactTimeStamp)
        return if (parentTaskHierarchy == null) {
            null
        } else {
            check(parentTaskHierarchy.notDeleted(exactTimeStamp))

            val parentTask = parentTaskHierarchy.parentTask
            check(parentTask.notDeleted(exactTimeStamp))

            parentTask
        }
    }

    fun getCustomTime(customTimeKey: CustomTimeKey) = if (customTimeKey.localCustomTimeId != null) {
        check(TextUtils.isEmpty(customTimeKey.remoteProjectId))
        check(TextUtils.isEmpty(customTimeKey.remoteCustomTimeId))

        localFactory.getLocalCustomTime(customTimeKey.localCustomTimeId)
    } else {
        check(!TextUtils.isEmpty(customTimeKey.remoteProjectId))
        check(!TextUtils.isEmpty(customTimeKey.remoteCustomTimeId))
        checkNotNull(remoteProjectFactory)

        remoteProjectFactory!!.getRemoteCustomTime(customTimeKey.remoteProjectId!!, customTimeKey.remoteCustomTimeId!!)
    }
}