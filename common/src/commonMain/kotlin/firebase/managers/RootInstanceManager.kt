package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.records.RootInstanceRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleKey

abstract class RootInstanceManager<T : ProjectType>(protected val taskRecord: TaskRecord<T>) : RootInstanceRecord.Parent {

    abstract var rootInstanceRecords: MutableMap<InstanceKey, Pair<RootInstanceRecord<T>, Boolean>>
        protected set

    val isSaved get() = rootInstanceRecords.any { it.value.second }

    abstract val databaseWrapper: DatabaseWrapper

    protected abstract fun getDatabaseCallback(): DatabaseCallback

    open val saveCallback: (() -> Unit)? = null

    fun save(): Boolean {
        val values = mutableMapOf<String, Any?>()

        val newRootInstanceRecords = rootInstanceRecords.mapValues {
            Pair(it.value.first, it.value.first.getValues(values))
        }.toMutableMap()

        ErrorLogger.instance.log("RootInstanceManager.save values: $values")

        if (values.isNotEmpty()) {
            check(!isSaved)

            rootInstanceRecords = newRootInstanceRecords

            databaseWrapper.updateInstances(values, getDatabaseCallback())
        } else {
            saveCallback?.invoke()
        }

        return isSaved
    }

    fun newRootTaskRecord(
            instanceJson: InstanceJson,
            scheduleKey: ScheduleKey,
            customTimeId: CustomTimeId<T>?
    ) = RootInstanceRecord<T>(
            taskRecord,
            instanceJson,
            scheduleKey,
            customTimeId,
            this
    ).also {
        check(!rootInstanceRecords.containsKey(it.instanceKey))

        rootInstanceRecords[it.instanceKey] = Pair(it, false)
    }

    override fun removeRootInstanceRecord(instanceKey: InstanceKey) {
        check(rootInstanceRecords.containsKey(instanceKey))

        rootInstanceRecords.remove(instanceKey)
    }
}