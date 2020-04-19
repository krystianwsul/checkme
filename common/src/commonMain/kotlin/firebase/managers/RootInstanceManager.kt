package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.records.RootInstanceRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleKey

abstract class RootInstanceManager<T : ProjectType>(
        protected val taskRecord: TaskRecord<T>
) : RootInstanceRecord.Parent {

    abstract val rootInstanceRecords: MutableMap<InstanceKey, RootInstanceRecord<T>>

    var isSaved = false
        protected set

    abstract val databaseWrapper: DatabaseWrapper

    fun save(values: MutableMap<String, Any?>) {
        val myValues = mutableMapOf<String, Any?>()

        val newIsSaved = rootInstanceRecords.map { it.value.getValues(myValues) }.any { it }

        ErrorLogger.instance.log("RootInstanceManager.save values: $myValues")

        check(newIsSaved == myValues.isNotEmpty())
        if (myValues.isNotEmpty()) {
            check(!isSaved)

            isSaved = newIsSaved
        }

        values += myValues.mapKeys { "${DatabaseWrapper.KEY_INSTANCES}/${it.key}" }
    }

    fun newRootInstanceRecord(
            instanceJson: InstanceJson,
            scheduleKey: ScheduleKey,
            scheduleCustomTimeId: CustomTimeId<T>?
    ) = RootInstanceRecord(
            taskRecord,
            instanceJson,
            scheduleKey,
            scheduleCustomTimeId,
            this
    ).also {
        check(!rootInstanceRecords.containsKey(it.instanceKey))

        rootInstanceRecords[it.instanceKey] = it
    }

    override fun removeRootInstanceRecord(instanceKey: InstanceKey) {
        check(rootInstanceRecords.containsKey(instanceKey))

        rootInstanceRecords.remove(instanceKey)
    }
}