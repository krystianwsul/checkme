package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.criteria.Assignable
import com.krystianwsul.common.firebase.models.interval.ScheduleInterval
import com.krystianwsul.common.firebase.models.interval.Type
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.locker.LockerManager
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*
import com.soywiz.klock.days

class Instance<T : ProjectType> private constructor(val task: Task<T>, private var data: Data<T>) : Assignable {

    companion object {

        fun getNotificationId(
                scheduleDate: Date,
                scheduleJsonTime: JsonTime,
                taskKey: TaskKey,
        ): Int {
            return getNotificationId(
                    scheduleDate,
                    TimeDescriptor.fromJsonTime(scheduleJsonTime),
                    taskKey.run { Pair(projectKey.key, taskId) },
            )
        }

        /*
        I'm going to make some assumptions here:
            1. I won't live past a hundred years
            2. scheduleYear is between 2016 and 2088 (that way the algorithm should be fine during my lifetime)
            3. scheduleCustomTimeId is between 1 and 10,000
            4. hash looping past Integer.MAX_VALUE isn't likely to cause collisions
         */

        /**
         * todo: The whole scheduleCustomTimeId isn't guaranteed to be unique, but I don't feel like migrating
         * InstanceShownRecord right now.  Move it to Paper later, and make all this strongly typed
         */

        // todo just hash a data object
        fun getNotificationId(
                scheduleDate: Date,
                scheduleTimeDescriptor: TimeDescriptor,
                taskKey: Pair<String, String>,
        ): Int {
            var hash = scheduleDate.month
            hash += 12 * scheduleDate.day
            hash += 12 * 31 * (scheduleDate.year - 2015)
            hash += 12 * 31 * 73 * scheduleTimeDescriptor.hashCode()
            hash += 12 * 31 * 73 * 13 * taskKey.hashCode()

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

    val recordInstanceDateTime get() = data.recordInstanceDateTime

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
        get() = (instanceTime as? Time.Custom.Project<T>)?.key // todo customTime timepair

    private val instanceHourMinute get() = (instanceTime as? Time.Normal)?.hourMinute

    val notificationId get() = getNotificationId(scheduleDate, JsonTime.fromTime<T>(scheduleTime), taskKey)

    val hidden get() = data.hidden

    // scenario already covered by task/schedule relevance
    val customTimeKey get() = data.customTimeKey

    val scheduleCustomTimeKey get() = data.scheduleCustomTimeKey

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

    private class ParentInstanceData<T : ProjectType>(val instance: Instance<T>, val doneCallback: () -> Unit)

    fun getTaskHierarchyParentInstance(): Instance<T>? {
        /**
         * The baseline here is getting the interval corresponding to the scheduleDateTime, as in
         * interval.start < scheduleDateTime, and interval.end > scheduleDateTime (if present).  But, since we
         * want changes in hierarchies to be retroactive, we remove the first condition, which leaves us with,
         * if (interval.end != null), interval.end >= child.scheduleDateTime.
         *
         * But that's also removed, since we're potentially walking backwards through ended intervals.
         *
         * Then, we tack on that changes shouldn't be applied to child instances after they're done, so that
         * adds the condition that,
         * if (child.done != null), interval.start < child.done
         *
         * -||- for parent.done
         */

        val interval = task.intervals
                .reversed()
                .asSequence()
                .filter { interval -> doneOffset?.let { interval.startExactTimeStampOffset < it } != false }
                .filter { interval ->
                    when (val type = interval.type) {
                        is Type.Child<T> -> {
                            val parentTaskHierarchy = type.getHierarchyInterval(interval).taskHierarchy
                            val parentTask = parentTaskHierarchy.parentTask
                            val parentInstance = parentTask.getInstance(scheduleDateTime)

                            parentInstance.doneOffset?.let {
                                interval.startExactTimeStampOffset < it
                            } != false
                        }
                        is Type.Schedule -> true
                        /**
                         * if this interval was created by removing a schedule (noScheduleOrParent == null), we should
                         * keep looking
                         */
                        is Type.NoSchedule<T> -> type.noScheduleOrParent == null // unexpected
                    }
                }
                .first()

        return when (val type = interval.type) {
            is Type.Child<T> -> {
                val parentTaskHierarchy = type.getHierarchyInterval(interval).taskHierarchy
                val parentTask = parentTaskHierarchy.parentTask
                parentTask.getInstance(scheduleDateTime)
            }
            is Type.Schedule<T> -> null
            is Type.NoSchedule<T> -> {
                if (type.noScheduleOrParent != null)
                    ErrorLogger.instance.logException(ParentInstanceException("unexpected interval type. child instance: $this, interval $interval, all intervals: ${task.intervals}"))
                // else then this is just the placeholder generated by IntervalBuilder

                null
            }
        }
    }

    private val parentInstanceProperty = invalidatableLazy {
        val parentInstance = when (val parentState = data.parentState) {
            ParentState.NoParent -> null
            is ParentState.Parent -> task.project.getInstance(parentState.parentInstanceKey)
            ParentState.Unset -> getTaskHierarchyParentInstance()
        }

        parentInstance?.let {
            ParentInstanceData(it, it.doneOffsetProperty.addCallback(::invalidateParentInstanceData))
        }
    }

    private class ParentInstanceException(message: String) : Exception(message)

    private val parentInstanceData by parentInstanceProperty
    val parentInstance get() = parentInstanceData?.instance

    constructor(task: Task<T>, instanceRecord: InstanceRecord<T>) : this(task, Data.Real(task, instanceRecord))
    constructor(task: Task<T>, scheduleDateTime: DateTime) : this(task, Data.Virtual(scheduleDateTime))

    init {
        addLazyCallbacks()

        doneOffsetProperty.addCallback(::invalidateParentInstanceData)
    }

    private lateinit var schedulesCallback: () -> Unit // this is because of how JS handles method references
    private lateinit var intervalsCallback: () -> Unit

    private fun addLazyCallbacks() {
        schedulesCallback = task.scheduleIntervalsProperty.addCallback(matchingScheduleIntervalsProperty::invalidate)
        intervalsCallback = task.intervalsProperty.addCallback(::invalidateParentInstanceData)
    }

    private fun removeLazyCallbacks() {
        task.scheduleIntervalsProperty.removeCallback(schedulesCallback)
        task.intervalsProperty.removeCallback(intervalsCallback)

        tearDownParentInstanceData()
    }

    private fun tearDownParentInstanceData() {
        if (parentInstanceProperty.isInitialized()) {
            removeFromParentInstanceHierarchyContainer()

            parentInstanceData?.apply {
                instance.doneOffsetProperty.removeCallback(doneCallback)
            }

            parentInstanceProperty.invalidate()
        }
    }

    private fun invalidateParentInstanceData() {
        tearDownParentInstanceData()

        addToParentInstanceHierarchyContainer()
    }

    fun exists() = (data is Data.Real)

    fun getChildInstances(): List<Instance<T>> {
        val instanceLocker = getInstanceLocker()
        instanceLocker?.childInstances?.let { return it }

        val scheduleDateTime = scheduleDateTime

        val taskHierarchyChildInstances = task.childHierarchyIntervals
                .asSequence()
                /**
                 * todo it seems to me that this `filter` should be redundant with the check in getParentInstance, but a
                 * test fails if I remove it.
                 */
                .filter { interval -> // once an instance is done, we don't want subsequently added task hierarchies contributing to it
                    doneOffset?.let { it > interval.taskHierarchy.startExactTimeStampOffset } != false
                }
                .map {
                    it.taskHierarchy
                            .childTask
                            .getInstance(scheduleDateTime)
                }
                .filter { it.parentInstance?.instanceKey == instanceKey }
                .toList()

        val instanceHierarchyChildInstances = task.instanceHierarchyContainer.getChildInstances(instanceKey)

        val childInstances = (taskHierarchyChildInstances + instanceHierarchyChildInstances).distinct()

        instanceLocker?.childInstances = childInstances

        return childInstances
    }

    fun isRootInstance() = parentInstance == null

    fun getDisplayData() = if (isRootInstance()) instanceDateTime else null

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

    fun isVisible(now: ExactTimeStamp.Local, visibilityOptions: VisibilityOptions): Boolean {
        val instanceLocker = getInstanceLocker()?.also { check(it.now == now) }

        instanceLocker?.isVisible
                ?.get(visibilityOptions)
                ?.let { return it }

        val isVisible = isVisibleHelper(now, visibilityOptions)

        instanceLocker?.isVisible?.put(visibilityOptions, isVisible)

        return isVisible
    }

    private fun matchesSchedule() = getMatchingScheduleIntervals(true).isNotEmpty()

    private fun isInvisibleBecauseOfEndData(now: ExactTimeStamp.Local) =
            task.run { !notDeleted(now) && endData!!.deleteInstances && done == null }

    data class VisibilityOptions(
            val hack24: Boolean = false, // show done roots for 24 hours. Ignored for children
            val ignoreHidden: Boolean = false,
            val assumeChildOfVisibleParent: Boolean = false,
            val assumeRoot: Boolean = false,
    ) {

        init {
            check(!(hack24 && assumeChildOfVisibleParent))
            check(!(assumeChildOfVisibleParent && assumeRoot))
        }
    }

    private fun isVisibleHelper(now: ExactTimeStamp.Local, visibilityOptions: VisibilityOptions): Boolean {
        if (!visibilityOptions.ignoreHidden && data.hidden) return false

        if (isInvisibleBecauseOfEndData(now)) return false

        fun checkVisibilityForRoot(): Boolean {
            if (!isValidlyCreated()) return false
            val done = done ?: return true

            val cutoff = if (visibilityOptions.hack24)
                ExactTimeStamp.Local(now.toDateTimeSoy() - 1.days)
            else
                now

            return done > cutoff
        }

        val parentInstance = parentInstance

        return when {
            visibilityOptions.assumeChildOfVisibleParent -> {
                checkNotNull(parentInstance)

                true
            }
            visibilityOptions.assumeRoot -> {
                check(parentInstance == null)

                checkVisibilityForRoot()
            }
            parentInstance != null -> parentInstance.isVisible(now, visibilityOptions)
            else -> checkVisibilityForRoot()
        }
    }

    private fun isValidlyCreated() = exists() || matchesSchedule()

    override fun toString() = "${super.toString()} name: $name, schedule time: $scheduleDateTime, instance time: $instanceDateTime, done: $done"

    fun hide() {
        check(!data.hidden)

        createInstanceRecord().instanceRecord.hidden = true
    }

    fun unhide() {
        check(data.hidden)

        (data as Data.Real<T>).instanceRecord.hidden = false
    }

    fun getParentName() = parentInstance?.name ?: task.project.name

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
            dateTime: DateTime?,
    ) {
        if (dateTime == recordInstanceDateTime) return

        createInstanceRecord().instanceRecord.let {
            it.instanceDate = dateTime?.date

            it.instanceJsonTime = dateTime?.time?.let {
                task.project
                        .getOrCopyTime(ownerKey, it)
                        .let { JsonTime.fromTime<T>(it) }
            }
        }

        shownHolder.forceShown(shownFactory).notified = false
    }

    private fun createInstanceRecord(): Data.Real<T> {
        if (data !is Data.Real<T>) {
            data = Data.Real(
                    task,
                    task.createRemoteInstanceRecord(this)
            )

            addToParentInstanceHierarchyContainer()
        }

        return data as Data.Real<T>
    }

    fun setDone(shownFactory: ShownFactory, done: Boolean, now: ExactTimeStamp.Local) {
        if (done) {
            createInstanceRecord().instanceRecord.let {
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

        task.deleteInstance(this)

        (data as Data.Real<*>).instanceRecord.delete()
    }

    // todo use for all CreateTaskActivity schedule hints.  Either filter by current, or add non-current to create task data
    fun getCreateTaskTimePair(now: ExactTimeStamp.Local, privateProject: PrivateProject): TimePair {
        val instanceTimePair = instanceTime.timePair

        return if (instanceTimePair.customTimeKey != null) {
            val customTime = task.project.getCustomTime(instanceTimePair.customTimeKey.customTimeId)

            val privateCustomTime = when (customTime) {
                is SharedCustomTime -> {
                    val ownerKey = privateProject.projectKey.toUserKey()

                    if (customTime.ownerKey == ownerKey) {
                        val privateCustomTimeKey = CustomTimeKey.Project.Private(
                                ownerKey.toPrivateProjectKey(),
                                customTime.privateKey!!,
                        )

                        privateProject.getCustomTime(privateCustomTimeKey)
                    } else {
                        null
                    }
                }
                is PrivateCustomTime -> customTime
                else -> throw UnsupportedOperationException()
            }

            privateCustomTime?.takeIf { it.current(now) }
                    ?.let { TimePair(it.key) }
                    ?: TimePair(customTime.getHourMinute(instanceDate.dayOfWeek))
        } else {
            instanceTimePair
        }
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
        if (!isRootInstance()) return listOf()

        return getMatchingScheduleIntervals(false).map { it.schedule.assignedTo }
                .distinct()
                .singleOrEmpty()
                .orEmpty()
                .let(task.project::getAssignedTo)
                .map { it.value }
    }

    fun setParentState(newParentState: ParentState) {
        if (parentState == newParentState) return

        newParentState.parentInstanceKey?.let { check(it.taskKey.projectKey == task.project.projectKey) }

        createInstanceRecord().parentState = newParentState

        invalidateParentInstanceData()
    }

    private var addedToParentInstanceHierarchyContainer = false

    fun addToParentInstanceHierarchyContainer() {
        if (!addedToParentInstanceHierarchyContainer && exists()) {
            parentInstance?.let { parentInstance ->
                val parentTask = parentInstance.task

                parentTask.instanceHierarchyContainer.addChildInstance(this)
            }
        }

        addedToParentInstanceHierarchyContainer = true
    }

    private fun removeFromParentInstanceHierarchyContainer() {
        if (addedToParentInstanceHierarchyContainer && parentInstanceProperty.isInitialized() && exists()) {
            parentInstance?.let { parentInstance ->
                val parentTask = parentInstance.task

                parentTask.instanceHierarchyContainer.removeChildInstance(this)
            }
        }

        addedToParentInstanceHierarchyContainer = false
    }

    fun canAddSubtask(now: ExactTimeStamp.Local, hack24: Boolean = false): Boolean {
        // can't add to deleted tasks
        if (!task.current(now)) return false

        // obviously we can't add instances to an invisible instance.
        if (!isVisible(now, VisibilityOptions(hack24 = hack24))) return false

        // if it's a child, we also shouldn't add instances if the parent is done
        return parentInstance?.canAddSubtask(now, hack24) ?: true
    }

    private sealed class Data<T : ProjectType> {

        abstract val scheduleDate: Date
        abstract val instanceDate: Date

        abstract val scheduleTime: Time
        abstract val instanceTime: Time

        abstract val recordInstanceDateTime: DateTime?

        abstract val done: Long?
        abstract val doneOffset: Double?

        abstract val hidden: Boolean

        abstract val scheduleHourMinute: HourMinute?

        abstract val customTimeKey: CustomTimeKey?

        abstract val scheduleCustomTimeKey: CustomTimeKey.Project<*>? // todo customtime project

        abstract val parentState: ParentState

        class Real<T : ProjectType>(
                private val task: Task<T>,
                val instanceRecord: InstanceRecord<T>,
        ) : Data<T>() {

            override val scheduleDate get() = instanceRecord.run { Date(scheduleYear, scheduleMonth, scheduleDay) }

            override val instanceDate get() = instanceRecord.instanceDate ?: scheduleDate

            val scheduleJsonTime get() = JsonTime.fromTimePair<T>(instanceRecord.scheduleKey.scheduleTimePair)

            override val scheduleTime get() = scheduleJsonTime.toTime(task.project)

            private val recordInstanceTime: Time? get() = instanceRecord.instanceJsonTime?.toTime(task.project)

            override val instanceTime get() = recordInstanceTime ?: scheduleTime

            override val recordInstanceDateTime: DateTime?
                get() {
                    val date = instanceRecord.instanceDate
                    val time = recordInstanceTime

                    return if (date != null) {
                        checkNotNull(time)

                        DateTime(date, time)
                    } else {
                        check(time == null)

                        return null
                    }
                }

            override val done get() = instanceRecord.done
            override val doneOffset get() = instanceRecord.doneOffset

            override val hidden get() = instanceRecord.hidden

            override val scheduleHourMinute
                get() = instanceRecord.scheduleHour?.let { HourMinute(it, instanceRecord.scheduleMinute!!) }

            override val customTimeKey get() = instanceRecord.instanceJsonTime?.getCustomTimeKey(task.project)

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

            override val customTimeKey: CustomTimeKey? = null

            override val scheduleCustomTimeKey = scheduleTime.timePair.customTimeKey

            override val parentState = ParentState.Unset

            override val recordInstanceDateTime: DateTime? = null
        }
    }

    private inner class ShownHolder {

        private var first = true
        private var shown: Shown? = null

        fun getShown(shownFactory: ShownFactory): Shown? {
            if (first)
                shown = shownFactory.getShown<T>(taskKey, scheduleDateTime)
            return shown
        }

        fun forceShown(shownFactory: ShownFactory): Shown {
            if (getShown(shownFactory) == null)
                shown = shownFactory.createShown<T>(taskKey.taskId, scheduleDateTime, taskKey.projectKey)
            return shown!!
        }
    }

    interface Shown {

        var notified: Boolean
        var notificationShown: Boolean
    }

    interface ShownFactory {

        fun <T : ProjectType> createShown(
                remoteTaskId: String,
                scheduleDateTime: DateTime,
                projectId: ProjectKey<*>,
        ): Shown

        fun getShown(
                projectId: ProjectKey<*>,
                taskId: String,
                scheduleYear: Int,
                scheduleMonth: Int,
                scheduleDay: Int,
                scheduleJsonTime: JsonTime,
        ): Shown?

        fun <T : ProjectType> getShown(taskKey: TaskKey, scheduleDateTime: DateTime): Shown? {
            return getShown(
                    taskKey.projectKey,
                    taskKey.taskId,
                    scheduleDateTime.date.year,
                    scheduleDateTime.date.month,
                    scheduleDateTime.date.day,
                    JsonTime.fromTime<T>(scheduleDateTime.time),
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
