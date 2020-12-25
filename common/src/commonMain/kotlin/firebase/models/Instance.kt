package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.criteria.Assignable
import com.krystianwsul.common.firebase.models.interval.ScheduleInterval
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.locker.LockerManager
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*
import com.soywiz.klock.days

class Instance<T : ProjectType> private constructor(val task: Task<T>, private var data: Data<T>) : Assignable {

    companion object {

        fun getNotificationId(
                scheduleDate: Date,
                scheduleCustomTimeKey: CustomTimeKey<*>?,
                scheduleHourMinute: HourMinute?,
                taskKey: TaskKey,
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
                taskKey: Pair<String, String>,
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

    val instanceKey by lazy { InstanceKey(taskKey, scheduleKey) }

    val scheduleKey by lazy { ScheduleKey(scheduleDate, TimePair(scheduleCustomTimeKey, data.scheduleHourMinute)) }

    val scheduleDate get() = data.scheduleDate
    val scheduleTime get() = data.scheduleTime
    val scheduleDateTime get() = DateTime(scheduleDate, data.scheduleTime)

    val instanceDate get() = data.instanceDate
    val instanceTime get() = data.instanceTime
    val instanceDateTime get() = DateTime(instanceDate, instanceTime)

    val taskKey by lazy { task.taskKey }

    private val doneProperty = invalidatableLazyCallbacks { data.done?.let { ExactTimeStamp.Local(it) } }
    val done by doneProperty

    private val doneOffsetProperty = invalidatableLazyCallbacks {
        data.done?.let { ExactTimeStamp.Offset.fromOffset(it, data.doneOffset) }
    }
    val doneOffset by doneOffsetProperty

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

    private val hierarchyExactTimeStampEndRangeProperty = invalidatableLazy {
        listOfNotNull(
                task.endExactTimeStampOffset?.let { Pair(it.minusOne(), "task end") },
                doneOffset?.let { Pair(it.minusOne(), "done") }
        ).minByOrNull { it.first }
    }

    private val hierarchyExactTimeStampEndRange by hierarchyExactTimeStampEndRangeProperty

    val parentState get() = data.parentState

    private val matchingScheduleIntervalsProperty = invalidatableLazy {
        val exactTimeStamp = scheduleDateTime.toLocalExactTimeStamp().toOffset()

        task.getScheduleDateTimes(
                exactTimeStamp,
                exactTimeStamp.plusOne(),
                originalDateTime = true,
                checkOldestVisible = false
        ).toList()
    }
    private val matchingScheduleIntervals by matchingScheduleIntervalsProperty

    constructor(task: Task<T>, instanceRecord: InstanceRecord<T>) : this(task, Data.Real(task, instanceRecord))
    constructor(task: Task<T>, scheduleDateTime: DateTime) : this(task, Data.Virtual(scheduleDateTime))

    init {
        addLazyCallbacks()
    }

    private fun addLazyCallbacks() {
        task.endDataProperty.addCallback(hierarchyExactTimeStampEndRangeProperty::invalidate)
        doneProperty.addCallback(hierarchyExactTimeStampEndRangeProperty::invalidate)
        doneOffsetProperty.addCallback(hierarchyExactTimeStampEndRangeProperty::invalidate)
        task.scheduleIntervalsProperty.addCallback(matchingScheduleIntervalsProperty::invalidate)
    }

    private fun removeLazyCallbacks() {
        task.endDataProperty.removeCallback(hierarchyExactTimeStampEndRangeProperty::invalidate)
        doneProperty.removeCallback(hierarchyExactTimeStampEndRangeProperty::invalidate)
        doneOffsetProperty.removeCallback(hierarchyExactTimeStampEndRangeProperty::invalidate)
        task.scheduleIntervalsProperty.removeCallback(matchingScheduleIntervalsProperty::invalidate)
    }

    fun exists() = (data is Data.Real)

    fun getChildInstances(now: ExactTimeStamp.Local): List<Instance<T>> {
        val instanceLocker = getInstanceLocker()?.also { check(it.now == now) }
        instanceLocker?.childInstances?.let { return it }

        val scheduleDateTime = scheduleDateTime

        val taskHierarchyChildInstances = task.childHierarchyIntervals
                .asSequence()
                .map {
                    it.taskHierarchy
                            .childTask
                            .getInstance(scheduleDateTime)
                }
                .filter { !it.isInvisibleBecauseOfEndData(now) }
                .filter {
                    it.getParentInstance(now)
                            ?.instance
                            ?.instanceKey == instanceKey
                }
                .toList()

        val instanceHierarchyChildInstances = task.instanceHierarchyContainer.getChildInstances(instanceKey)

        val childInstances = (taskHierarchyChildInstances + instanceHierarchyChildInstances).distinct()

        instanceLocker?.childInstances = childInstances

        return childInstances
    }

    private fun getHierarchyExactTimeStamp(now: ExactTimeStamp.Local): Pair<ExactTimeStamp, String> {
        if (now < task.startExactTimeStampOffset) return task.startExactTimeStampOffset to "task start"

        hierarchyExactTimeStampEndRange?.let { if (now > it.first) return it }

        return now to "now"
    }

    fun isRootInstance(now: ExactTimeStamp.Local) = getParentInstance(now) == null

    fun getDisplayData(now: ExactTimeStamp.Local) = if (isRootInstance(now)) instanceDateTime else null

    private fun createInstanceHierarchy(now: ExactTimeStamp.Local): Data.Real<T> {
        (data as? Data.Real)?.let { return it }

        getParentInstance(now)?.instance?.createInstanceHierarchy(now)

        return createInstanceRecord()
    }

    // this does not account for whether or not this is a rootInstance
    private fun getMatchingScheduleIntervals(checkOldestVisible: Boolean): List<ScheduleInterval<T>> {
        val filtered = if (checkOldestVisible) {
            matchingScheduleIntervals.map {
                it.filter { it.second.schedule.isAfterOldestVisible(scheduleDateTime.toLocalExactTimeStamp()) }
            }
        } else {
            matchingScheduleIntervals
        }

        return filtered.singleOrEmpty()
                .orEmpty()
                .filter { it.first == scheduleDateTime }
                .map { it.second }
    }

    fun getOldestVisibles() = getMatchingScheduleIntervals(false).map { it.schedule.oldestVisible }

    private fun getInstanceLocker() = LockerManager.getInstanceLocker<T>(instanceKey)

    fun isVisible(now: ExactTimeStamp.Local, hack24: Boolean, ignoreHidden: Boolean = false): Boolean {
        val instanceLocker = getInstanceLocker()?.also { check(it.now == now) }

        instanceLocker?.let {
            val cachedIsVisible = if (hack24) it.isVisibleHack else it.isVisibleNoHack

            cachedIsVisible?.let { return it }
        }

        val isVisible = isVisibleHelper(now, hack24, ignoreHidden)

        instanceLocker?.let {
            if (hack24)
                it.isVisibleHack = isVisible
            else
                it.isVisibleNoHack = isVisible
        }

        return isVisible
    }

    private fun matchesSchedule() = getMatchingScheduleIntervals(true).isNotEmpty()

    private fun isInvisibleBecauseOfEndData(now: ExactTimeStamp.Local) =
            task.run { !notDeleted(now) && endData!!.deleteInstances && done == null }

    private fun isVisibleHelper(now: ExactTimeStamp.Local, hack24: Boolean, ignoreHidden: Boolean): Boolean {
        if (!ignoreHidden && data.hidden) return false

        if (isInvisibleBecauseOfEndData(now)) return false

        val parentInstance = getParentInstance(now)

        if (parentInstance != null) {
            return parentInstance.instance.isVisible(now, hack24)
        } else {
            if (!isValidlyCreated()) return false

            val done = done ?: return true

            val cutoff = if (hack24)
                ExactTimeStamp.Local(now.toDateTimeSoy() - 1.days)
            else
                now

            return done > cutoff
        }
    }

    private fun isVirtualParentInstance() = task.instanceHierarchyContainer
            .getParentScheduleKeys()
            .contains(scheduleKey)

    private fun isValidlyCreatedHierarchy(now: ExactTimeStamp.Local): Boolean = getParentInstance(now)?.instance
            ?.isValidlyCreatedHierarchy(now)
            ?: isValidlyCreated()

    private fun isValidlyCreated() = exists() || matchesSchedule() || isVirtualParentInstance()

    data class ParentInstanceData<T : ProjectType>(val instance: Instance<T>, val viaParentState: Boolean)

    fun getParentInstance(now: ExactTimeStamp.Local): ParentInstanceData<T>? {
        val instanceLocker = getInstanceLocker()?.also { check(it.now == now) }

        instanceLocker?.parentInstanceWrapper?.let { return it.value }

        val parentInstanceData = when (val parentState = data.parentState) {
            ParentState.NoParent -> null
            is ParentState.Parent -> {
                val parentInstance = task.project.getInstance(parentState.parentInstanceKey)

                ParentInstanceData(parentInstance, true)
            }
            ParentState.Unset -> {
                val hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now).first

                val parentTask = task.getParentTask(hierarchyExactTimeStamp)

                if (parentTask == null) {
                    null
                } else {
                    check(parentTask.notDeletedOffset(hierarchyExactTimeStamp))

                    /**
                     * todo I think this should also factor in whether or not an instance in the hierarchy exists, which is a
                     * slightly different issue than being reachable from the main screen.  But I'll leave well enough alone
                     * for now.  There would be a discrepancy when accessing an instance in a different way
                     * (ShowTaskInstancesActivity is the only one that comes to mind).
                     *
                     * I think a parent is truly eligible if:
                     * 1. It, or any instance in the hierarchy above it, exists
                     * 2. The root instance matches a schedule
                     *
                     * Yet another reason to consider checking if task.getInstances() contains the instance.
                     */
                    parentTask.getInstance(scheduleDateTime)
                            .takeIf { it.isValidlyCreatedHierarchy(now) }
                            ?.let { ParentInstanceData(it, false) }
                }
            }
        }

        instanceLocker?.parentInstanceWrapper = NullableWrapper(parentInstanceData)

        return parentInstanceData
    }

    override fun toString() = "${super.toString()} name: $name, schedule time: $scheduleDateTime instance time: $instanceDateTime, done: $done"

    fun hide(now: ExactTimeStamp.Local) {
        check(!data.hidden)

        createInstanceHierarchy(now).instanceRecord.hidden = true
    }

    fun getParentName(now: ExactTimeStamp.Local) = getParentInstance(now)?.instance
            ?.name
            ?: task.project.name

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

    fun fixNotificationShown(shownFactory: ShownFactory, now: ExactTimeStamp.Local) {
        if (done != null || instanceDateTime.toLocalExactTimeStamp() > now)
            getShown(shownFactory)?.notified = false
    }

    fun setInstanceDateTime(
            shownFactory: ShownFactory,
            ownerKey: UserKey,
            dateTime: DateTime,
            now: ExactTimeStamp.Local,
    ) {
        check(isRootInstance(now))

        if (dateTime == instanceDateTime) return

        createInstanceHierarchy(now).instanceRecord.let {
            it.instanceDate = dateTime.date

            it.instanceJsonTime = task.project
                    .getOrCopyTime(ownerKey, dateTime.time)
                    .let {
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
            task,
            task.createRemoteInstanceRecord(this)
    ).also {
        data = it

        doneProperty.invalidate()
        doneOffsetProperty.invalidate()
    }

    fun setDone(shownFactory: ShownFactory, done: Boolean, now: ExactTimeStamp.Local) {
        if (done) {
            createInstanceHierarchy(now).instanceRecord.let {
                it.done = now.long
                it.doneOffset = now.offset
            }

            getShown(shownFactory)?.notified = false
        } else {
            (data as Data.Real<*>).instanceRecord.let {
                it.done = null
                it.doneOffset = null
            }
        }

        doneProperty.invalidate()
        doneOffsetProperty.invalidate()
    }

    fun delete() {
        check(data is Data.Real<*>)

        removeLazyCallbacks()

        removeVirtualParents()

        task.deleteInstance(this)

        (data as Data.Real<*>).instanceRecord.delete()
    }

    // todo use for all CreateTaskActivity schedule hints.  Either filter by current, or add non-current to create task data
    fun getCreateTaskTimePair(ownerKey: UserKey): TimePair {
        val instanceTimePair = instanceTime.timePair
        val shared = instanceTimePair.customTimeKey as? CustomTimeKey.Shared

        return if (shared != null) {
            val sharedCustomTime = task.project.getCustomTime(shared.customTimeId) as SharedCustomTime

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

    fun isGroupChild(now: ExactTimeStamp.Local) = getParentInstance(now)?.viaParentState ?: false

    fun onTaskEndChanged() {
        hierarchyExactTimeStampEndRangeProperty.invalidate()
    }

    fun getSequenceDate(bySchedule: Boolean) = if (bySchedule) scheduleDateTime else instanceDateTime

    fun fixOffsets() {
        done?.let {
            val instanceRecord = (data as Data.Real<*>).instanceRecord

            if (instanceRecord.doneOffset == null) {
                instanceRecord.doneOffset = it.offset

                doneOffsetProperty.invalidate()
            }
        }
    }

    override fun getAssignedTo(now: ExactTimeStamp.Local): List<ProjectUser> {
        if (!isRootInstance(now)) return listOf()

        return getMatchingScheduleIntervals(false).map { it.schedule.assignedTo }
                .distinct()
                .singleOrEmpty()
                .orEmpty()
                .let(task.project::getAssignedTo)
                .map { it.value }
    }

    fun setParentState(newParentState: ParentState, now: ExactTimeStamp.Local) {
        if (parentState == newParentState) return

        removeVirtualParents()
        createInstanceHierarchy(now).parentState = newParentState
        addVirtualParents()
    }

    fun addVirtualParents() {
        parentState.parentInstanceKey?.let {
            val parentTask = task.project.getTaskForce(it.taskKey.taskId)

            parentTask.instanceHierarchyContainer.addChildInstance(this)
            parentTask.getInstance(task.project.getDateTime(it.scheduleKey)).addVirtualParents()
        }
    }

    private fun removeVirtualParents() {
        parentState.parentInstanceKey?.let {
            val parentTask = task.project.getTaskForce(it.taskKey.taskId)

            parentTask.instanceHierarchyContainer.removeChildInstance(this)
            parentTask.getInstance(task.project.getDateTime(it.scheduleKey)).removeVirtualParents()
        }
    }

    private sealed class Data<T : ProjectType> {

        abstract val scheduleDate: Date
        abstract val instanceDate: Date

        abstract val scheduleTime: Time
        abstract val instanceTime: Time

        abstract val done: Long?
        abstract val doneOffset: Double?

        abstract val hidden: Boolean

        abstract val scheduleHourMinute: HourMinute?

        abstract val customTimeKey: Pair<ProjectKey<T>, CustomTimeId<T>>?

        abstract val scheduleCustomTimeKey: CustomTimeKey<*>?

        abstract val parentState: ParentState

        class Real<T : ProjectType>(
                private val task: Task<T>,
                val instanceRecord: InstanceRecord<T>,
        ) : Data<T>() {

            fun getCustomTime(customTimeId: CustomTimeId<T>) = task.project.getCustomTime(customTimeId)

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
            override val doneOffset get() = instanceRecord.doneOffset

            override val hidden get() = instanceRecord.hidden

            override val scheduleHourMinute
                get() = instanceRecord.scheduleHour?.let { HourMinute(it, instanceRecord.scheduleMinute!!) }

            override val customTimeKey
                get() = instanceRecord.instanceJsonTime?.let {
                    (it as? JsonTime.Custom)?.let { Pair(task.project.projectKey, it.id) }
                }

            override val scheduleCustomTimeKey
                get() = instanceRecord.scheduleKey
                        .scheduleTimePair
                        .customTimeKey

            override var parentState: ParentState
                get() {
                    val parentInstanceKey = instanceRecord.parentInstanceKey

                    return when {
                        instanceRecord.noParent -> ParentState.NoParent
                        parentInstanceKey != null -> ParentState.Parent(parentInstanceKey)
                        else -> ParentState.Unset
                    }
                }
                set(value) {
                    when (value) {
                        ParentState.Unset -> {
                            instanceRecord.noParent = false
                            instanceRecord.parentInstanceKey = null
                        }
                        ParentState.NoParent -> {
                            instanceRecord.noParent = true
                            instanceRecord.parentInstanceKey = null
                        }
                        is ParentState.Parent -> {
                            instanceRecord.noParent = false
                            instanceRecord.parentInstanceKey = value.parentInstanceKey
                        }
                    }
                }
        }

        class Virtual<T : ProjectType>(scheduleDateTime: DateTime) : Data<T>() {

            override val scheduleDate = scheduleDateTime.date
            override val instanceDate = scheduleDate

            override val scheduleTime = scheduleDateTime.time
            override val instanceTime = scheduleTime

            override val done: Long? = null
            override val doneOffset: Double? = null

            override val hidden = false

            override val scheduleHourMinute = scheduleTime.timePair.hourMinute

            override val customTimeKey: Pair<ProjectKey<T>, CustomTimeId<T>>? = null

            override val scheduleCustomTimeKey = scheduleTime.timePair.customTimeKey

            override val parentState = ParentState.Unset
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
                scheduleMinute: Int?,
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

    sealed class ParentState {

        abstract val noParent: Boolean
        abstract val parentInstanceKey: InstanceKey?

        object Unset : ParentState() {

            override val noParent = false
            override val parentInstanceKey: InstanceKey? = null
        }

        object NoParent : ParentState() {

            override val noParent = true
            override val parentInstanceKey: InstanceKey? = null
        }

        data class Parent(override val parentInstanceKey: InstanceKey) : ParentState() {

            override val noParent = false
        }
    }
}
