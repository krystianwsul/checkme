package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*
import com.soywiz.klock.days

class Instance<T : ProjectType> private constructor(
        val project: Project<T>,
        val task: Task<T>,
        private var data: Data<T>
) {

    companion object {

        fun getNotificationId(
                scheduleDate: Date,
                scheduleCustomTimeKey: CustomTimeKey<*>?,
                scheduleHourMinute: HourMinute?,
                taskKey: TaskKey
        ) = getNotificationId(
                scheduleDate,
                scheduleCustomTimeKey?.let { Pair(it.projectId.key, it.customTimeId.value) },
                scheduleHourMinute,
                taskKey.run { Pair(projectKey.key, taskId) }
        )

        /*
        I'm going to make some assumptions here:
            1. I won't live past a hundred years
            2. scheduleYear is between 2016 and 2088 (that way the algorithm should be fine during my lifetime)
            3. scheduleCustomTimeId is between 1 and 10,000
            4. hash looping past Integer.MAX_VALUE isn't likely to cause collisions
         */

        // todo just hash a data object
        fun getNotificationId(
                scheduleDate: Date,
                scheduleCustomTimeData: Pair<String, String>?,
                scheduleHourMinute: HourMinute?,
                taskKey: Pair<String, String>
        ): Int {
            check(scheduleCustomTimeData == null != (scheduleHourMinute == null))

            var hash = scheduleDate.month
            hash += 12 * scheduleDate.day
            hash += 12 * 31 * (scheduleDate.year - 2015)

            if (scheduleCustomTimeData == null) {
                hash += 12 * 31 * 73 * (scheduleHourMinute!!.hour + 1)
                hash += 12 * 31 * 73 * 24 * (scheduleHourMinute.minute + 1)
            } else {
                hash += 12 * 31 * 73 * 24 * 60 * scheduleCustomTimeData.hashCode()
            }

            @Suppress("INTEGER_OVERFLOW")
            hash += 12 * 31 * 73 * 24 * 60 * 10000 * taskKey.hashCode()

            return hash
        }
    }

    private val shownHolder = ShownHolder()

    val instanceKey get() = InstanceKey(taskKey, scheduleKey)

    val scheduleKey get() = ScheduleKey(scheduleDate, TimePair(scheduleCustomTimeKey, data.scheduleHourMinute))

    val scheduleDate get() = data.scheduleDate
    val scheduleTime get() = data.scheduleTime
    val scheduleDateTime get() = DateTime(scheduleDate, data.scheduleTime)

    val instanceDate get() = data.instanceDate
    val instanceTime get() = data.instanceTime
    val instanceDateTime get() = DateTime(instanceDate, instanceTime)

    val taskKey by lazy { task.taskKey }

    val done get() = data.done?.let { ExactTimeStamp(it) }

    val name get() = task.name

    val instanceTimePair get() = TimePair(instanceCustomTimeKey, instanceHourMinute)

    @Suppress("UNCHECKED_CAST")
    val instanceCustomTimeKey
        get() = (instanceTime as? Time.Custom<T>)?.key

    private val instanceHourMinute get() = (instanceTime as? Time.Normal)?.hourMinute

    val notificationId get() = getNotificationId(scheduleDate, scheduleCustomTimeKey, data.scheduleHourMinute, taskKey)

    val hidden get() = data.hidden

    // scenario already covered by task/schedule relevance
    val customTimeKey get() = data.customTimeKey

    val scheduleCustomTimeKey get() = data.scheduleCustomTimeKey

    constructor(
            project: Project<T>,
            task: Task<T>,
            instanceRecord: InstanceRecord<T>
    ) : this(project, task, Data.Real(project, instanceRecord))

    constructor(
            project: Project<T>,
            task: Task<T>,
            scheduleDateTime: DateTime
    ) : this(project, task, Data.Virtual(scheduleDateTime))

    fun exists() = (data is Data.Real)

    fun getChildInstances(now: ExactTimeStamp): List<Pair<Instance<T>, TaskHierarchy<*>>> {
        val hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now).first

        val scheduleDateTime = scheduleDateTime

        return task.childHierarchyIntervals
                .asSequence()
                .mapNotNull { it.takeIf { it.notDeleted(hierarchyExactTimeStamp) }?.taskHierarchy }
                .filter { it.notDeleted(hierarchyExactTimeStamp) && it.childTask.notDeleted(hierarchyExactTimeStamp) }
                .map { Pair(it.childTask.getInstance(scheduleDateTime), it) }
                .filter {
                    it.first
                            .getParentInstance(now)
                            ?.instanceKey == instanceKey
                }
                .associateBy { it.first.instanceKey } // I think this is weeding out duplicates
                .values
                .toList()
    }

    private fun getHierarchyExactTimeStamp(now: ExactTimeStamp) = listOfNotNull(
            Pair(now, "now"),
            //Pair(scheduleDateTime.timeStamp.toExactTimeStamp(), "schedule"), this was messing up single instance lists
            task.endExactTimeStamp?.let { Pair(it.minusOne(), "task end") },
            done?.let { Pair(it.minusOne(), "done") }
    ).minBy { it.first }!!

    fun isRootInstance(now: ExactTimeStamp) = getParentInstance(now) == null

    fun getDisplayData(now: ExactTimeStamp) = if (isRootInstance(now)) instanceDateTime else null

    private fun createInstanceHierarchy(now: ExactTimeStamp): Data.Real<*> {
        (data as? Data.Real)?.let {
            return it
        }

        getParentInstance(now)?.createInstanceHierarchy(now)

        return createInstanceRecord()
    }

    fun isVisible(now: ExactTimeStamp, hack24: Boolean): Boolean {
        return if (isRootInstance(now) &&
                task.getOldestVisible()?.let { scheduleDate < it } == true &&
                !exists()
        ) {
            false
        } else {
            isVisibleHelper(now, hack24)
        }
    }

    private fun matchesSchedule() = task.scheduleIntervals.any {
        it.matchesScheduleDateTime(scheduleDateTime)
    }

    private fun isVisibleHelper(now: ExactTimeStamp, hack24: Boolean): Boolean {
        if (data.hidden)
            return false

        if (task.run { !notDeleted(now) && getEndData()!!.deleteInstances && done == null })
            return false

        val parentInstance = getParentInstance(now)
        if (parentInstance != null)
            return parentInstance.isVisible(now, hack24)

        if (!exists() && !matchesSchedule())
            return false

        val done = done ?: return true

        val cutoff = if (hack24)
            ExactTimeStamp(now.toDateTimeSoy() - 1.days)
        else
            now

        return done > cutoff
    }

    private fun isEligibleParentInstance(now: ExactTimeStamp): Boolean =
            getParentInstance(now)?.isEligibleParentInstance(now) ?: (exists() || matchesSchedule())

    fun getParentInstance(now: ExactTimeStamp): Instance<T>? {
        val hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now)

        val parentTask = task.getParentTask(hierarchyExactTimeStamp.first) ?: return null

        return if (parentTask.notDeleted(hierarchyExactTimeStamp.first)) {
            parentTask.getInstance(scheduleDateTime).takeIf { it.isEligibleParentInstance(now) }
        } else {
            fun message(task: Task<*>) = "name: ${task.name}, start: ${task.startExactTimeStamp}, end: ${task.endExactTimeStamp}"

            // todo remove after June 17th 2020
            ErrorLogger.instance.logException(ParentInstanceException("instance: " + toString() + ", task: " + message(task) + ", parentTask: " + message(parentTask) + ", hierarchy: " + hierarchyExactTimeStamp))

            null
        }
    }

    override fun toString() = "${super.toString()} name: $name, schedule time: $scheduleDateTime instance time: $instanceDateTime, done: $done"

    private class ParentInstanceException(message: String) : Exception(message)

    val ordinal
        get() = data.ordinal ?: task.startExactTimeStamp
                .long
                .toDouble()

    fun setOrdinal(ordinal: Double, now: ExactTimeStamp) {
        createInstanceHierarchy(now)

        (data as Data.Real).instanceRecord.ordinal = ordinal
    }

    fun hide(now: ExactTimeStamp) {
        check(!data.hidden)

        createInstanceHierarchy(now).instanceRecord.hidden = true
    }

    fun getParentName(now: ExactTimeStamp) = getParentInstance(now)?.name ?: project.name

    fun getShown(shownFactory: ShownFactory) = shownHolder.getShown(shownFactory)

    /*
    Has the instance's notification been dismissed? Meaningful only if the instance is a root
    instance, in the past, and not done.  If either of the last two are changed, this flag gets
    reset.  As far as being a root instance, there's no simple way to catch that moment.
     */
    fun getNotified(shownFactory: ShownFactory) = getShown(shownFactory)?.notified == true

    fun setNotified(shownFactory: ShownFactory, notified: Boolean) {
        shownHolder.forceShown(shownFactory).notified = notified
    }

    // Is the notification visible?
    fun getNotificationShown(shownFactory: ShownFactory) = getShown(shownFactory)?.notificationShown == true

    fun setNotificationShown(shownFactory: ShownFactory, notificationShown: Boolean) {
        shownHolder.forceShown(shownFactory).notificationShown = notificationShown
    }

    fun fixNotificationShown(shownFactory: ShownFactory, now: ExactTimeStamp) {
        if (done != null || instanceDateTime.toExactTimeStamp() > now)
            getShown(shownFactory)?.notified = false
    }

    fun setInstanceDateTime(
            shownFactory: ShownFactory,
            ownerKey: UserKey,
            dateTime: DateTime,
            now: ExactTimeStamp
    ) {
        check(isRootInstance(now))

        createInstanceHierarchy(now)

        @Suppress("UNCHECKED_CAST")
        (data as Data.Real<T>).instanceRecord.let {
            it.instanceDate = dateTime.date

            it.instanceJsonTime = project.getOrCopyTime(ownerKey, dateTime.time).let {
                @Suppress("UNCHECKED_CAST")
                when (it) {
                    is Time.Custom<*> -> JsonTime.Custom(it.key.customTimeId as CustomTimeId<T>)
                    is Time.Normal -> JsonTime.Normal(it.hourMinute)
                }
            }
        }

        shownHolder.forceShown(shownFactory).notified = false
    }

    private fun createInstanceRecord() = Data.Real(
            project,
            task.createRemoteInstanceRecord(this)
    ).also { data = it }

    fun setDone(shownFactory: ShownFactory, done: Boolean, now: ExactTimeStamp) {
        if (done) {
            createInstanceHierarchy(now).instanceRecord.done = now.long

            getShown(shownFactory)?.notified = false
        } else {
            (data as Data.Real<*>).instanceRecord.done = null
        }
    }

    fun delete() {
        check(data is Data.Real<*>)

        task.deleteInstance(this)

        (data as Data.Real<*>).instanceRecord.delete()
    }

    // todo use for all CreateTaskActivity schedule hints.  Either filter by current, or add non-current to create task data
    fun getCreateTaskTimePair(ownerKey: UserKey): TimePair {
        val instanceTimePair = instanceTime.timePair
        val shared = instanceTimePair.customTimeKey as? CustomTimeKey.Shared

        return if (shared != null) {
            val sharedCustomTime = project.getCustomTime(shared.customTimeId) as SharedCustomTime

            if (sharedCustomTime.ownerKey == ownerKey) {
                val privateCustomTimeKey = CustomTimeKey.Private(ownerKey.toPrivateProjectKey(), sharedCustomTime.privateKey!!)

                TimePair(privateCustomTimeKey)
            } else {
                val hourMinute = sharedCustomTime.getHourMinute(instanceDate.dayOfWeek)

                TimePair(hourMinute)
            }
        } else {
            instanceTimePair
        }
    }

    private sealed class Data<T : ProjectType> {

        abstract val scheduleDate: Date
        abstract val instanceDate: Date

        abstract val scheduleTime: Time
        abstract val instanceTime: Time

        abstract val done: Long?

        abstract val hidden: Boolean

        abstract val scheduleHourMinute: HourMinute?

        abstract val customTimeKey: Pair<ProjectKey<T>, CustomTimeId<T>>?

        abstract val scheduleCustomTimeKey: CustomTimeKey<*>?

        abstract val ordinal: Double?

        class Real<T : ProjectType>(
                private val project: Project<T>,
                val instanceRecord: InstanceRecord<T>
        ) : Data<T>() {

            fun getCustomTime(customTimeId: CustomTimeId<T>) = project.getCustomTime(customTimeId)

            override val scheduleDate get() = instanceRecord.run { Date(scheduleYear, scheduleMonth, scheduleDay) }

            override val instanceDate get() = instanceRecord.instanceDate ?: scheduleDate

            override val scheduleTime
                get() = instanceRecord.run {
                    scheduleCustomTimeId?.let { getCustomTime(it) }
                            ?: Time.Normal(scheduleHour!!, scheduleMinute!!)
                }

            override val instanceTime
                get() = instanceRecord.instanceJsonTime
                        ?.let {
                            when (it) {
                                is JsonTime.Custom -> getCustomTime(it.id)
                                is JsonTime.Normal -> Time.Normal(it.hourMinute)
                            }
                        }
                        ?: scheduleTime

            override val done get() = instanceRecord.done

            override val hidden get() = instanceRecord.hidden

            override val scheduleHourMinute
                get() = instanceRecord.scheduleHour?.let { HourMinute(it, instanceRecord.scheduleMinute!!) }

            override val customTimeKey
                get() = instanceRecord.instanceJsonTime?.let {
                    (it as? JsonTime.Custom)?.let { Pair(project.projectKey, it.id) }
                }

            override val scheduleCustomTimeKey
                get() = instanceRecord.scheduleKey
                        .scheduleTimePair
                        .customTimeKey

            override val ordinal get() = instanceRecord.ordinal
        }

        class Virtual<T : ProjectType>(scheduleDateTime: DateTime) : Data<T>() {

            override val scheduleDate = scheduleDateTime.date
            override val instanceDate = scheduleDate

            override val scheduleTime = scheduleDateTime.time
            override val instanceTime = scheduleTime

            override val done: Long? = null

            override val hidden = false

            override val scheduleHourMinute = scheduleTime.timePair.hourMinute

            override val customTimeKey: Pair<ProjectKey<T>, CustomTimeId<T>>? = null

            override val scheduleCustomTimeKey = scheduleTime.timePair.customTimeKey

            override val ordinal: Double? = null
        }
    }

    private inner class ShownHolder {

        private var first = true
        private var shown: Shown? = null

        fun getShown(shownFactory: ShownFactory): Shown? {
            if (first)
                shown = shownFactory.getShown(taskKey, scheduleDateTime)
            return shown
        }

        fun forceShown(shownFactory: ShownFactory): Shown {
            if (getShown(shownFactory) == null)
                shown = shownFactory.createShown(taskKey.taskId, scheduleDateTime, taskKey.projectKey)
            return shown!!
        }
    }

    interface Shown {

        var notified: Boolean
        var notificationShown: Boolean
    }

    interface ShownFactory {

        fun createShown(remoteTaskId: String, scheduleDateTime: DateTime, projectId: ProjectKey<*>): Shown

        fun getShown(
                projectId: ProjectKey<*>,
                taskId: String,
                scheduleYear: Int,
                scheduleMonth: Int,
                scheduleDay: Int,
                scheduleCustomTimeId: CustomTimeId<*>?,
                scheduleHour: Int?,
                scheduleMinute: Int?
        ): Shown?

        fun getShown(taskKey: TaskKey, scheduleDateTime: DateTime): Shown? {
            val (customTimeId, hour, minute) = scheduleDateTime.time
                    .timePair
                    .destructureRemote()

            return getShown(
                    taskKey.projectKey,
                    taskKey.taskId,
                    scheduleDateTime.date.year,
                    scheduleDateTime.date.month,
                    scheduleDateTime.date.day,
                    customTimeId,
                    hour,
                    minute
            )
        }
    }
}
