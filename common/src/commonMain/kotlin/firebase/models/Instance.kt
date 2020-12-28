package com.krystianwsul.common.firebase.models


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

    data class ParentInstanceData<T : ProjectType>(val instance: Instance<T>, val viaParentState: Boolean)

    private val parentInstanceDataProperty = invalidatableLazy {
        val parentInstanceData = when (val parentState = data.parentState) {
            ParentState.NoParent -> null
            is ParentState.Parent -> {
                val parentInstance = task.project.getInstance(parentState.parentInstanceKey)

                ParentInstanceData(parentInstance, true)
            }
            ParentState.Unset -> {
                /**
                 * 1. timestamp should be no oldest than the task start
                 * 2. timestamp should be no newer than task end
                 * 3. timestamp should be no newer than this instance's done
                 * 4. barring all else, use task's most recent interval
                 *
                 * So, assuming we can't mark the instance as done before its corresponding task is created, we can
                 * change this to:
                 *
                 * 1. if done is set, get interval for that time.
                 * 2. else, use most recent interval (also, add utility method for this in task)
                 */

                val interval = done?.let { task.getInterval(it) } ?: task.getMostRecentInterval()

                val parentTask = (interval.type as? Type.Child<T>)?.getHierarchyInterval(interval)
                        ?.taskHierarchy
                        ?.parentTask

                if (parentTask == null) {
                    null
                } else {
                    /**
                     * we also check if the parent task is done before all this went down, to prevent adding to finished
                     * lists.  So, we should compare that "done" against when the interval started - as in, did the interval
                     * first get added, or did the parent first get marked as done?
                     *
                     * Not sure which type of inequality to use here, but I don't think it really matters outside of
                     * tests.
                     *
                     * I think this logic is flawed for using `doneOffset`, in that the candidate instance should be
                     * considered the parent, but the child instance invisible.  The goal is for the child to not be
                     * visible anywhere, but I don't know if this makes a difference in practice, given the current
                     * logic elsewhere.
                     */

                    parentTask.getInstance(scheduleDateTime)
                            .takeIf { it.doneOffset?.let { it > interval.startExactTimeStampOffset } != false }
                            ?.takeIf { it.isValidlyCreatedHierarchy() }
                            ?.let { ParentInstanceData(it, false) }
                }
            }
        }

        parentInstanceData?.also {
            it.instance
                    .doneOffsetProperty
                    .addCallback(::invalidateParentInstanceData)
        }
    }

    val parentInstanceData by parentInstanceDataProperty

    constructor(task: Task<T>, instanceRecord: InstanceRecord<T>) : this(task, Data.Real(task, instanceRecord))
    constructor(task: Task<T>, scheduleDateTime: DateTime) : this(task, Data.Virtual(scheduleDateTime))

    init {
        addLazyCallbacks()

        doneOffsetProperty.addCallback(::invalidateParentInstanceData)
    }

    private fun addLazyCallbacks() {
        task.scheduleIntervalsProperty.addCallback(matchingScheduleIntervalsProperty::invalidate)
        task.intervalsProperty.addCallback(::invalidateParentInstanceData)
    }

    private fun removeLazyCallbacks() {
        task.scheduleIntervalsProperty.removeCallback(matchingScheduleIntervalsProperty::invalidate)
        task.intervalsProperty.removeCallback(::invalidateParentInstanceData)

        tearDownParentInstanceData()
    }

    private fun tearDownParentInstanceData() {
        if (parentInstanceDataProperty.isInitialized()) {
            removeVirtualParents()

            parentInstanceData?.instance
                    ?.doneOffsetProperty
                    ?.removeCallback(::invalidateParentInstanceData)
        }
    }

    private fun invalidateParentInstanceData() {
        tearDownParentInstanceData()

        parentInstanceDataProperty.invalidate()

        addVirtualParents()
    }

    fun exists() = (data is Data.Real)

    fun getChildInstances(): List<Instance<T>> {
        val instanceLocker = getInstanceLocker()
        instanceLocker?.childInstances?.let { return it }

        val scheduleDateTime = scheduleDateTime

        val taskHierarchyChildInstances = task.childHierarchyIntervals
                .asSequence()
                .map {
                    it.taskHierarchy
                            .childTask
                            .getInstance(scheduleDateTime)
                }
                .filter {
                    it.parentInstanceData
                            ?.instance
                            ?.instanceKey == instanceKey
                }
                .toList()

        val instanceHierarchyChildInstances = task.instanceHierarchyContainer.getChildInstances(instanceKey)

        val childInstances = (taskHierarchyChildInstances + instanceHierarchyChildInstances).distinct()

        instanceLocker?.childInstances = childInstances

        return childInstances
    }

    fun isRootInstance() = parentInstanceData == null

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

        val parentInstanceData = parentInstanceData

        return when {
            visibilityOptions.assumeChildOfVisibleParent -> {
                checkNotNull(parentInstanceData)

                true
            }
            visibilityOptions.assumeRoot -> {
                check(parentInstanceData == null)

                checkVisibilityForRoot()
            }
            parentInstanceData != null -> parentInstanceData.instance.isVisible(now, visibilityOptions)
            else -> checkVisibilityForRoot()
        }
    }

    private fun isVirtualParentInstance() = task.instanceHierarchyContainer
            .getParentScheduleKeys()
            .contains(scheduleKey)

    private fun isValidlyCreatedHierarchy(): Boolean = parentInstanceData?.instance
            ?.isValidlyCreatedHierarchy()
            ?: isValidlyCreated()

    private fun isValidlyCreated() = exists() || matchesSchedule() || isVirtualParentInstance()

    override fun toString() = "${super.toString()} name: $name, schedule time: $scheduleDateTime instance time: $instanceDateTime, done: $done"

    fun hide() {
        check(!data.hidden)

        createInstanceRecord().instanceRecord.hidden = true
    }

    fun getParentName() = parentInstanceData?.instance
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
    ) {
        check(isRootInstance())

        if (dateTime == instanceDateTime) return

        createInstanceRecord().instanceRecord.let {
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

    private fun createInstanceRecord(): Data.Real<T> {
        if (data !is Data.Real<T>) {
            data = Data.Real(
                    task,
                    task.createRemoteInstanceRecord(this)
            )

            addVirtualParents()
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

        tearDownParentInstanceData()

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

    fun isGroupChild() = parentInstanceData?.viaParentState ?: false

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

        createInstanceRecord().parentState = newParentState

        invalidateParentInstanceData()
    }

    fun addVirtualParents() {
        if (exists()) {
            parentInstanceData?.instance?.let { parentInstance ->
                val parentTask = parentInstance.task

                parentTask.instanceHierarchyContainer.addChildInstance(this)
                parentTask.getInstance(task.project.getDateTime(parentInstance.scheduleKey)).addVirtualParents()
            }
        }
    }

    private fun removeVirtualParents() {
        if (parentInstanceDataProperty.isInitialized() && exists()) {
            parentInstanceData?.instance?.let { parentInstance ->
                val parentTask = parentInstance.task

                parentTask.instanceHierarchyContainer.removeChildInstance(this)
                parentTask.getInstance(task.project.getDateTime(parentInstance.scheduleKey)).removeVirtualParents()
            }
        }
    }

    fun canAddSubtask(now: ExactTimeStamp.Local, hack24: Boolean = false): Boolean {
        // can't add to deleted tasks
        if (!task.current(now)) return false

        // obviously we can't add instances to an invisible instance.
        if (!isVisible(now, VisibilityOptions(hack24 = hack24))) return false

        // if it's a child, we also shouldn't add instances if the parent is done
        return parentInstanceData?.instance
                ?.canAddSubtask(now, hack24)
                ?: true
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
