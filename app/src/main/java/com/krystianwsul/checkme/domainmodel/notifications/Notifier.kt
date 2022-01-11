package com.krystianwsul.checkme.domainmodel.notifications

import android.os.Build
import android.util.Log
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.GroupType
import com.krystianwsul.checkme.gui.main.DebugFragment
import com.krystianwsul.checkme.ticks.Ticker
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.users.ProjectOrdinalManager
import com.krystianwsul.common.relevance.CustomTimeRelevance
import com.krystianwsul.common.relevance.Irrelevant
import com.krystianwsul.common.time.DateTimeSoy
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.singleOrEmpty
import com.soywiz.klock.DateTime
import com.soywiz.klock.Time
import com.soywiz.klock.days
import com.soywiz.klock.hours

class Notifier(private val domainFactory: DomainFactory, private val notificationWrapper: NotificationWrapper) {

    companion object {

        // To prevent spam if there's a huge backlog
        private const val MAX_NOTIFICATIONS_Q = 10

        const val TEST_IRRELEVANT = false

        // duplicate of logic in Instance.shouldShowNotification
        private fun Sequence<Instance>.filterNotifications(domainFactory: DomainFactory, now: ExactTimeStamp.Local) =
            filter {
                it.done == null &&
                        !it.getNotified(domainFactory.shownFactory) &&
                        it.isAssignedToMe(now, domainFactory.myUserFactory.user)
            }

        fun getNotificationInstances(domainFactory: DomainFactory, now: ExactTimeStamp.Local) =
            domainFactory.getRootInstances(
                null,
                now.toOffset().plusOne(),
                now,
            ).filterNotifications(domainFactory, now)

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
                projectKey = projectKey,
            ).filterNotifications(domainFactory, now).toList()
        }
    }

    fun updateNotifications(now: ExactTimeStamp.Local, params: Params) {
        val (sourceName, silent, _, clear) = params

        Preferences.tickLog.logLineDate("updateNotifications start $params")

        notificationWrapper.hideTemporary(Ticker.TICK_NOTIFICATION_ID, sourceName)

        val notificationDatas = mutableListOf<NotificationData>()

        fun cancelNotificationDatas() {
            notificationDatas.filterIsInstance<NotificationData.Cancel>().forEach {
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

        fun cancelInstance(instanceId: Int) {
            notificationDatas += NotificationData.Cancel(instanceId)
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
            ).also {
                DebugFragment.logDone("Notifier.updateNotifications getRootInstances end")
            }.filterNotifications(domainFactory, now)
            DebugFragment.logDone("Notifier.updateNotifications filterNotifications end")

            var needsOneExtra = true
            val allNotificationInstances =
                notificationInstanceSequence.map { (it.instanceDateTime.toLocalExactTimeStamp() <= now) to it }
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
                    .groupBy({ it.first }, { it.second })

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
            cancelInstance(Instance.getNotificationId(instanceShownEntry.key))

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
                    cancelInstance(instance.notificationId)
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
                    cancelInstance(it.notificationId)
                }

                fun showSummary() { // Android notification group thingy
                    Preferences.tickLog.logLineHour("showing summary")
                    NotificationWrapper.instance.notifyGroup(notificationInstances.values, true, now)
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

                // hide everything first, then show.  If applicable, FILO summary
                when {
                    notifications.size > MAX_NOTIFICATIONS_Q -> {
                        //hide
                        if (!wereMaxShown) shownInstanceKeys.cancelNotifications() // else group was already shown

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
                        showSummary()

                        notifies.forEach { Preferences.tickLog.logLineHour("showing/updating '" + it.instance.name + "'") }

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

        data class Cancel(val instanceId: Int) : NotificationData()

        data class Notify(val instance: Instance, val silent: Boolean = true) : NotificationData()
    }

    private fun getNotifications(notifies: List<NotificationData.Notify>) = GroupTypeFactory.getGroupTypeTree(
        notifies.map { GroupTypeFactory.InstanceDescriptor(it.instance, it.silent) },
        GroupType.GroupingMode.Time(),
    ).flatMap { it.getNotifications() }

    private fun notifyInstances(notifications: List<GroupTypeFactory.Notification>, now: ExactTimeStamp.Local) {
        notifications.forEach {
            when (it) {
                is GroupTypeFactory.Notification.Instance ->
                    notificationWrapper.notifyInstance(domainFactory.deviceDbInfo, it.instance, it.silent, now)
                is GroupTypeFactory.Notification.Project -> {
                    it.instances.forEach { NotificationWrapper.instance.cancelNotification(it.notificationId) }

                    val ordinal = domainFactory.myUserFactory
                        .user
                        .getProjectOrdinalManager(it.project)
                        .getOrdinal(it.project, ProjectOrdinalManager.Key(it.instances))

                    notificationWrapper.notifyProject(it.project, it.instances, it.timeStamp, it.silent, now, ordinal)
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

            val userCustomTimeRelevances = domainFactory.run {
                friendsFactory.userMap
                    .map { it.value.value } +
                        myUserFactory.user
            }
                .flatMap { it.customTimes.values }
                .associate { it.key to CustomTimeRelevance(it) }

            val results = Irrelevant.setIrrelevant(
                { domainFactory.rootTasksFactory.rootTasks },
                userCustomTimeRelevances,
                { domainFactory.projectsFactory.projects },
                domainFactory.rootTasksFactory,
                exactTimeStamp,
                listOf(domainFactory.myUserFactory.user),
            )

            results.irrelevantExistingInstances
                .sortedBy { it.scheduleDateTime }
                .forEach { Log.e("asdf", "magic irrelevant instance: $it") }

            results.irrelevantSchedules
                .sortedBy { it.startExactTimeStamp }
                .forEach { Log.e("asdf", "magic irrelevant schedule, schedule: $it, task: ${it.topLevelTask}") }

            throw Exception("Irrelevant.setIrrelevant write prevented")
        }

        val relevantInstanceShownKeys = domainFactory.getRootInstances(null, now.toOffset().plusOne(), now)
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