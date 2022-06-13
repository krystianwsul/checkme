package com.krystianwsul.checkme.domainmodel.notifications

import android.os.Build
import android.util.Log
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.GroupType
import com.krystianwsul.checkme.gui.main.DebugFragment
import com.krystianwsul.checkme.ticks.Ticker
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.search.FilterResult
import com.krystianwsul.common.firebase.models.search.SearchContext
import com.krystianwsul.common.firebase.models.users.ProjectOrdinalManager
import com.krystianwsul.common.relevance.CustomTimeRelevance
import com.krystianwsul.common.relevance.Irrelevant
import com.krystianwsul.common.time.DateTimeSoy
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.*
import com.soywiz.klock.DateTime
import com.soywiz.klock.Time
import com.soywiz.klock.days
import com.soywiz.klock.hours

class Notifier(private val domainFactory: DomainFactory, private val notificationWrapper: NotificationWrapper) {

    companion object {

        // To prevent spam if there's a huge backlog
        private const val MAX_NOTIFICATIONS_Q = 10

        const val TEST_IRRELEVANT = false

        private val searchCriteria = SearchCriteria(showAssignedToOthers = false, showDone = false)

        private fun newSearchContext(now: ExactTimeStamp.Local, domainFactory: DomainFactory) = SearchContext.startSearch(
            searchCriteria,
            now,
            domainFactory.myUserFactory.user,
        )

        // duplicate of logic in Instance.shouldShowNotification
        private fun Sequence<Pair<Instance, FilterResult>>.filterNotifications(domainFactory: DomainFactory) =
            map { it.first }.filter {
                val tracker = TimeLogger.startIfLogDone("Notifier filter")

                val ret = !it.getNotified(domainFactory.shownFactory)

                tracker?.stop()

                ret
            }

        fun getNotificationInstances(domainFactory: DomainFactory, now: ExactTimeStamp.Local) =
            domainFactory.getRootInstances(
                null,
                now.toOffset().plusOne(),
                now,
                newSearchContext(now, domainFactory),
            ).filterNotifications(domainFactory)

        fun getNotificationInstances(
            domainFactory: DomainFactory,
            now: ExactTimeStamp.Local,
            projectKey: ProjectKey.Shared,
            timeStamp: TimeStamp,
        ): List<Instance> {
            val offset = timeStamp.toLocalExactTimeStamp().toOffset()

            return domainFactory.getRootInstances(
                offset,
                offset.plusOne(),
                now,
                newSearchContext(now, domainFactory),
                projectKey = projectKey,
            ).filterNotifications(domainFactory)
                .filter { it.groupByProject }
                .toList()
        }

        fun setIrrelevant(domainFactory: DomainFactory, exactTimeStamp: ExactTimeStamp.Local): Irrelevant.Result {
            val userCustomTimeRelevances = domainFactory.run {
                friendsFactory.userMap
                    .map { it.value.value } +
                        myUserFactory.user
            }
                .flatMap { it.customTimes.values }
                .associate { it.key to CustomTimeRelevance(it) }

            return Irrelevant.setIrrelevant(
                { domainFactory.rootTasksFactory.rootTasks },
                userCustomTimeRelevances,
                { domainFactory.projectsFactory.projects },
                domainFactory.rootTasksFactory,
                exactTimeStamp,
                listOf(domainFactory.myUserFactory.user),
            )
        }
    }

    fun updateNotifications(now: ExactTimeStamp.Local, params: Params) {
        val (sourceName, silent, _, clear) = params

        Preferences.tickLog.logLineDate("updateNotifications start $params")

        notificationWrapper.hideTemporary(Ticker.TICK_NOTIFICATION_ID, sourceName)

        val notificationDatas = mutableListOf<NotificationData>()

        fun cancelNotificationDatas() {
            notificationDatas.filterIsInstance<NotificationData.Cancel>().forEach {
                Preferences.fcmLog.logLineHour("canceling notification for " + it.instanceName)
                notificationWrapper.cancelNotification(it.instanceId)
            }
        }

        fun getNotifications() = getNotifications(notificationDatas.filterIsInstance<NotificationData.Notify>())

        fun notifyInstance(instance: Instance, silent: Boolean) {
            notificationDatas += NotificationData.Notify(instance, silent)
        }

        fun updateInstance(instance: Instance) {
            notificationDatas += NotificationData.Notify(instance)
        }

        fun cancelInstance(instanceId: Int, instanceName: String) {
            notificationDatas += NotificationData.Cancel(instanceId, instanceName)
        }

        val notificationInstances: Map<InstanceKey, Instance>
        val nextAlarmInstance: Instance?
        if (clear) {
            notificationInstances = mapOf()
            nextAlarmInstance = null
        } else {
            DebugFragment.logDone("Notifier.updateNotifications getRootInstances start")

            val notificationInstanceSequence = domainFactory.getRootInstances(
                null,
                null,
                now,
                newSearchContext(now, domainFactory),
            )
                .also { DebugFragment.logDone("Notifier.updateNotifications getRootInstances end") }
                .filterNotifications(domainFactory)

            DebugFragment.logDone("Notifier.updateNotifications filterNotifications end")

            var needsOneExtra = true

            TimeLogger.clear()

            val mainTracker = TimeLogger.startIfLogDone("whole thing")

            val allNotificationInstances =
                notificationInstanceSequence.map {
                    val tracker = TimeLogger.startIfLogDone("Notifier mapDateTime")

                    val ret = (it.instanceDateTime.toLocalExactTimeStamp() <= now) to it

                    tracker?.stop()

                    ret
                }
                    .takeWhile { (beforeNow, _) ->
                        when {
                            beforeNow -> true
                            needsOneExtra -> {
                                needsOneExtra = false

                                true
                            }
                            else -> false
                        }
                    }
                    .toList()
                    .let {
                        val tracker = TimeLogger.startIfLogDone("Notifier group")

                        it.groupBy({ it.first }, { it.second }).also { tracker?.stop() }
                    }

            mainTracker?.stop()

            DebugFragment.logDone("Notifier.updateNotifications finish sequence")
            DebugFragment.logDone("TimeLogger:\n" + TimeLogger.printToString())
            DebugFragment.logDone("TimeLogger sum:\n" + TimeLogger.sumExcluding(mainTracker?.key ?: ""))

            notificationInstances = allNotificationInstances[true].orEmpty().associateBy { it.instanceKey }
            nextAlarmInstance = allNotificationInstances[false].orEmpty().singleOrEmpty()
        }

        check(notificationInstances.values.all { it.task.dependenciesLoaded })

        Preferences.tickLog.logLineHour(
            "notification instances: " + notificationInstances.values.joinToString(", ") { it.name }
        )

        val instanceShownPairs = domainFactory.shownFactory
            .instanceShownMap
            .entries
            .filter { it.value.notificationShown }
            .map { it to domainFactory.tryGetTask(it.key.taskKey) }

        instanceShownPairs.filter { it.second == null }.forEach { (instanceShownEntry, _) ->
            cancelInstance(Instance.getNotificationId(instanceShownEntry.key), "unknown")

            instanceShownEntry.value.notificationShown = false
        }

        val shownInstanceKeys = instanceShownPairs.filter { it.second != null }
            .map { (instanceShownEntry, _) -> instanceShownEntry.key }
            .toSet()

        val showInstanceKeys = notificationInstances.keys - shownInstanceKeys

        Preferences.tickLog.logLineHour(
            "shown instances: " + shownInstanceKeys.joinToString(", ") {
                domainFactory.getInstance(it).name
            }
        )

        val hideInstanceKeys = shownInstanceKeys - notificationInstances.keys

        for (showInstanceKey in showInstanceKeys) {
            domainFactory.apply { getInstance(showInstanceKey).setNotificationShown(shownFactory, true) }
        }

        for (hideInstanceKey in hideInstanceKeys) {
            domainFactory.apply { getInstance(hideInstanceKey).setNotificationShown(shownFactory, false) }
        }

        Preferences.tickLog.logLineHour("silent? $silent")

        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> {
                if (notificationInstances.isEmpty()) {
                    Preferences.tickLog.logLineHour("hiding group")
                    NotificationWrapper.instance.cancelNotification(0)
                } else {
                    Preferences.tickLog.logLineHour("showing group")
                    NotificationWrapper.instance.notifyGroup(notificationInstances.values, true, now)
                }

                for (hideInstanceKey in hideInstanceKeys) {
                    val instance = domainFactory.getInstance(hideInstanceKey)
                    Preferences.tickLog.logLineHour("hiding '" + instance.name + "'")
                    cancelInstance(instance.notificationId, instance.name)
                }

                for (showInstanceKey in showInstanceKeys) {
                    val instance = notificationInstances.getValue(showInstanceKey)
                    Preferences.tickLog.logLineHour("showing '" + instance.name + "'")
                    notifyInstance(instance, silent)
                }

                val updateInstances = notificationInstances.values.filter { !showInstanceKeys.contains(it.instanceKey) }

                updateInstances.forEach {
                    Preferences.tickLog.logLineHour("updating '" + it.name + "' " + it.instanceDateTime)
                    updateInstance(it)
                }

                cancelNotificationDatas()

                val notifications = getNotifications()
                notifyInstances(notifications, now)
                cancelProjectNotifications(notifications)
            }
            else -> {
                /**
                 * in this section, "summary" is Android's summary notification thingy, whereas "group" is my own
                 * inbox-style notification
                 */

                fun Collection<InstanceKey>.cancelNotifications() = map(domainFactory::getInstance).forEach {
                    Preferences.tickLog.logLineHour("hiding '" + it.name + "'")
                    cancelInstance(it.notificationId, it.name)
                }

                val wereMaxShown = shownInstanceKeys.size > MAX_NOTIFICATIONS_Q

                fun hideGroupOrOld() {
                    Preferences.tickLog.logLineHour("hiding group")
                    NotificationWrapper.instance.cancelNotification(NotificationWrapperImpl.NOTIFICATION_ID_GROUP_NOT_SUMMARY)

                    hideInstanceKeys.cancelNotifications()
                }

                showInstanceKeys.forEach { notifyInstance(notificationInstances.getValue(it), silent) }

                // instances to be updated
                notificationInstances.values
                    .filter { !showInstanceKeys.contains(it.instanceKey) }
                    .forEach(::updateInstance)

                hideGroupOrOld()

                cancelNotificationDatas()

                val notifies = notificationDatas.filterIsInstance<NotificationData.Notify>()

                val notifications = getNotifications(notifies)

                fun showSummary() { // Android notification group thingy
                    Preferences.tickLog.logLineHour("showing summary")
                    NotificationWrapper.instance.notifyGroup(
                        notifications.filterIsInstance<GroupTypeFactory.Notification.Instance>().map { it.instance },
                        notifications.all { it.silent },
                        now,
                        true,
                        notifications.filterIsInstance<GroupTypeFactory.Notification.Project>(),
                    )
                }

                // hide everything first, then show.  If applicable, FILO summary
                when {
                    notifications.size > MAX_NOTIFICATIONS_Q -> {
                        //hide
                        if (!wereMaxShown) shownInstanceKeys.cancelNotifications() // else group was already shown

                        cancelNotificationDatas()

                        //show
                        showSummary()

                        Preferences.tickLog.logLineHour("showing group")

                        NotificationWrapper.instance.notifyGroup(
                            notifications.filterIsInstance<GroupTypeFactory.Notification.Instance>().map { it.instance },
                            silent,
                            now,
                            false,
                            notifications.filterIsInstance<GroupTypeFactory.Notification.Project>(),
                        )

                        cancelProjectNotifications(emptyList())
                    }
                    notificationInstances.isNotEmpty() -> {
                        notifies.forEach { Preferences.tickLog.logLineHour("showing/updating '" + it.instance.name + "'") }

                        /*
                        This summary lists individual instances instead of projects.  But, it's supposed to just be there to
                        allow grouping notifications; its content is never actually shown.
                         */

                        showSummary()

                        notifyInstances(notifications, now)
                        cancelProjectNotifications(notifications)
                    }
                    else -> {
                        check(notificationInstances.isEmpty())
                        check(showInstanceKeys.isEmpty())
                        check(shownInstanceKeys == hideInstanceKeys)

                        Preferences.tickLog.logLineHour("hiding summary")
                        NotificationWrapper.instance.cancelNotification(NotificationWrapperImpl.NOTIFICATION_ID_GROUP)

                        cancelProjectNotifications(emptyList())
                    }
                }
            }
        }

        if (!silent) Preferences.lastTick = now.long

        if (clear) {
            notificationWrapper.updateAlarm(null)
        } else {
            if (nextAlarmInstance != null) {
                val nextAlarmTimeStamp = nextAlarmInstance.instanceDateTime.timeStamp

                check(nextAlarmTimeStamp.toLocalExactTimeStamp() > now)

                notificationWrapper.updateAlarm(nextAlarmTimeStamp)

                Preferences.tickLog.logLineHour("next tick: $nextAlarmTimeStamp")
            }
        }

        if (params.tick || TEST_IRRELEVANT) {
            setIrrelevant(now)

            domainFactory.run { notificationStorage.deleteInstanceShown(getAllTasks().map { it.taskKey }.toSet()) }
        }
    }

    private sealed class NotificationData {

        data class Cancel(val instanceId: Int, val instanceName: String) : NotificationData()

        data class Notify(val instance: Instance, val silent: Boolean = true) : NotificationData()
    }

    private fun getNotifications(notifies: List<NotificationData.Notify>) = GroupTypeFactory.getGroupTypeTree(
        notifies.map { GroupTypeFactory.InstanceDescriptor(it.instance, it.silent, null) },
        GroupType.GroupingMode.Time(),
    ).flatMap { it.getNotifications() }

    private fun notifyInstances(notifications: List<GroupTypeFactory.Notification>, now: ExactTimeStamp.Local) {
        if (notifications.isEmpty()) return

        val notificationsWithOrdinals = notifications.map {
            val ordinal = when (it) {
                is GroupTypeFactory.Notification.Instance -> it.instance.ordinal
                is GroupTypeFactory.Notification.Project -> domainFactory.myUserFactory
                    .user
                    .getProjectOrdinalManager(it.project)
                    .getOrdinal(it.project, ProjectOrdinalManager.Key(it.instances))
            }

            it to ordinal
        }

        /*
        because negative numbers don't get sorted properly, we need to offset everything to be non-negative.  Subtract from
        ONE because the toString conversion behaves strangely for ZERO
         */

        val minOrdinal = notificationsWithOrdinals.map { it.second }
            .minOrNull()!!
            .let { if (it < Ordinal.ZERO) Ordinal.ONE - it else Ordinal.ZERO }

        notificationsWithOrdinals.forEach { (notification, ordinal) ->
            val adjustedOrdinal = minOrdinal + ordinal

            when (notification) {
                is GroupTypeFactory.Notification.Instance -> notificationWrapper.notifyInstance(
                    domainFactory.deviceDbInfo,
                    notification.instance,
                    notification.silent,
                    now,
                    adjustedOrdinal,
                )
                is GroupTypeFactory.Notification.Project -> {
                    notification.instances.forEach { NotificationWrapper.instance.cancelNotification(it.notificationId) }

                    notificationWrapper.notifyProject(
                        notification.project,
                        notification.instances,
                        notification.timeStamp,
                        notification.silent,
                        now,
                        adjustedOrdinal,
                    )
                }
            }
        }
    }

    private fun cancelProjectNotifications(notifications: List<GroupTypeFactory.Notification>) {
        val currentKeys = notifications.filterIsInstance<GroupTypeFactory.Notification.Project>()
            .map { ProjectNotificationKey(it.project.projectKey, it.timeStamp) }

        val oldKeys = domainFactory.notificationStorage.projectNotificationKeys

        (oldKeys - currentKeys).forEach {
            val notificationId = NotificationWrapperImpl.getProjectNotificationId(it.projectKey, it.timeStamp)

            NotificationWrapper.instance.cancelNotification(notificationId)
        }

        domainFactory.notificationStorage.projectNotificationKeys = currentKeys
    }

    private fun setIrrelevant(now: ExactTimeStamp.Local) {
        @Suppress("ConstantConditionIf")
        if (TEST_IRRELEVANT) {
            val tomorrow = (DateTimeSoy.now() + 1.days).date
            val dateTimeSoy = DateTime(tomorrow, Time(2.hours))
            val exactTimeStamp = ExactTimeStamp.Local(dateTimeSoy)

            val results = Companion.setIrrelevant(domainFactory, exactTimeStamp)

            results.irrelevantExistingInstances
                .sortedBy { it.scheduleDateTime }
                .forEach { Log.e("asdf", "magic irrelevant instance: $it") }

            results.irrelevantSchedules
                .sortedBy { it.startExactTimeStamp }
                .forEach { Log.e("asdf", "magic irrelevant schedule, schedule: $it, task: ${it.topLevelTask}") }

            results.irrelevantSchedules
                .sortedBy { it.startExactTimeStamp }
                .forEach { Log.e("asdf", "magic irrelevant task: $it") }

            throw Exception("Irrelevant.setIrrelevant write prevented")
        }

        val relevantInstanceShownKeys = domainFactory.getRootInstances(null, now.toOffset().plusOne(), now)
            .map { it.first }
            .mapNotNull { it.getShown(domainFactory.shownFactory)?.instanceKey }
            .toSet()

        val irrelevantInstanceShownEntries = domainFactory.shownFactory.instanceShownMap - relevantInstanceShownKeys

        irrelevantInstanceShownEntries.forEach { it.value.delete() }
    }

    data class Params(
        val sourceName: String = "other",
        val silent: Boolean = true,
        val tick: Boolean = false,
        val clear: Boolean = false,
    ) {

        companion object {

            fun merge(paramsList: List<Params>): Params? {
                if (paramsList.size < 2) return paramsList.singleOrEmpty()

                check(paramsList.none { it.clear })

                val sourceName = "merged: (" + paramsList.joinToString(", ") { it.sourceName } + ")"

                return Params(
                    sourceName,
                    paramsList.all { it.silent },
                    paramsList.any { it.tick },
                )
            }
        }
    }
}