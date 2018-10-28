package com.krystianwsul.checkme.domainmodel

import android.text.TextUtils // todo remove
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
import com.krystianwsul.checkme.utils.time.DateTime
import com.krystianwsul.checkme.utils.time.ExactTimeStamp

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

    fun getExistingInstanceIfPresent(taskKey: TaskKey, scheduleDateTime: DateTime): Instance? {
        val instanceKey = InstanceKey(taskKey, scheduleDateTime.date, scheduleDateTime.time.timePair)

        return getExistingInstanceIfPresent(instanceKey)
    }

    fun getExistingInstanceIfPresent(instanceKey: InstanceKey): Instance? {
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

    fun generateInstance(taskKey: TaskKey, scheduleDateTime: DateTime): Instance {
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
}