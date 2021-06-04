package com.krystianwsul.checkme.domainmodel

import android.os.Build
import android.util.Log
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapperImpl
import com.krystianwsul.checkme.ticks.Ticker
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.relevance.CustomTimeRelevance
import com.krystianwsul.common.relevance.Irrelevant
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateTimeSoy
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.singleOrEmpty
import com.soywiz.klock.DateTime
import com.soywiz.klock.Time
import com.soywiz.klock.days
import com.soywiz.klock.hours

class Notifier(private val domainFactory: DomainFactory, private val notificationWrapper: NotificationWrapper) {

    companion object {

        private const val MAX_NOTIFICATIONS_OLD = 3

        // To prevent spam if there's a huge backlog
        private const val MAX_NOTIFICATIONS_Q = 10

        fun getNotificationInstances(domainFactory: DomainFactory, now: ExactTimeStamp.Local): List<Instance> {
            return domainFactory.getRootInstances(null, now.toOffset().plusOne(), now /* 24 hack */)
                .filter {
                    it.done == null &&
                            !it.getNotified(domainFactory.localFactory) &&
                            it.isAssignedToMe(now, domainFactory.myUserFactory.user)
                }
                .toList()
        }
    }

    fun updateNotifications(now: ExactTimeStamp.Local, params: Params) {
        val (sourceName, silent, _, clear) = params

        Preferences.tickLog.logLineDate("updateNotifications start $params")

        notificationWrapper.hideTemporary(Ticker.TICK_NOTIFICATION_ID, sourceName)

        val notificationDatas = mutableListOf<NotificationData>()

        fun notifyInstance(instance: Instance, silent: Boolean) {
            notificationDatas += NotificationData.Notify(instance, silent)
        }

        fun updateInstance(instance: Instance) {
            notificationDatas += NotificationData.Notify(instance)
        }

        fun cancelInstance(instanceId: Int) {
            notificationDatas += NotificationData.Cancel(instanceId)
        }

        val notificationInstances = if (clear)
            mapOf()
        else
            getNotificationInstances(domainFactory, now).associateBy { it.instanceKey }

        Preferences.tickLog.logLineHour(
            "notification instances: " + notificationInstances.values.joinToString(", ") { it.name }
        )

        val instanceShownPairs = domainFactory.localFactory.instanceShownRecords
                .filter { it.notificationShown }
                .map { it to domainFactory.tryGetTask(it.taskKeyData) }

        instanceShownPairs.filter { it.second == null }.forEach { (instanceShownRecord, _) ->
            val scheduleDate = instanceShownRecord.run { Date(scheduleYear, scheduleMonth, scheduleDay) }

            cancelInstance(
                Instance.getNotificationId(
                    scheduleDate,
                    instanceShownRecord.scheduleTimeDescriptor,
                    instanceShownRecord.taskKeyData,
                )
            )
            instanceShownRecord.notificationShown = false
        }

        val shownInstanceKeys = instanceShownPairs.filter { it.second != null }
                .map { (instanceShownRecord, task) ->
                    val scheduleJsonTime =
                            instanceShownRecord.scheduleTimeDescriptor.toJsonTime(task!!.projectCustomTimeIdProvider)

                    val scheduleDate = instanceShownRecord.run { Date(scheduleYear, scheduleMonth, scheduleDay) }

                    InstanceKey(task.taskKey, scheduleDate, scheduleJsonTime.toTimePair(task.project))
                }
                .toSet()

        val showInstanceKeys = notificationInstances.keys - shownInstanceKeys

        Preferences.tickLog.logLineHour(
                "shown instances: " + shownInstanceKeys.joinToString(", ") {
                    domainFactory.getInstance(it).name
                }
        )

        val hideInstanceKeys = shownInstanceKeys - notificationInstances.keys

        for (showInstanceKey in showInstanceKeys) {
            domainFactory.getInstance(showInstanceKey).setNotificationShown(
                    domainFactory.localFactory,
                    true,
            )
        }

        for (hideInstanceKey in hideInstanceKeys) {
            domainFactory.getInstance(hideInstanceKey).setNotificationShown(
                    domainFactory.localFactory,
                    false,
            )
        }

        Preferences.tickLog.logLineHour("silent? $silent")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            if (notificationInstances.size > MAX_NOTIFICATIONS_OLD) { // show group
                if (shownInstanceKeys.size > MAX_NOTIFICATIONS_OLD) { // group shown
                    val silentParam =
                            if (showInstanceKeys.isNotEmpty() || hideInstanceKeys.isNotEmpty()) silent else true

                    NotificationWrapper.instance.notifyGroup(notificationInstances.values, silentParam, now)
                } else { // instances shown
                    for (shownInstanceKey in shownInstanceKeys) {
                        cancelInstance(domainFactory.getInstance(shownInstanceKey).notificationId)
                    }

                    notificationWrapper.notifyGroup(notificationInstances.values, silent, now)
                }
            } else { // show instances
                if (shownInstanceKeys.size > MAX_NOTIFICATIONS_OLD) { // group shown
                    NotificationWrapper.instance.cancelNotification(0)

                    for (instance in notificationInstances.values)
                        notifyInstance(instance, silent)
                } else { // instances shown
                    for (hideInstanceKey in hideInstanceKeys) {
                        cancelInstance(domainFactory.getInstance(hideInstanceKey).notificationId)
                    }

                    for (showInstanceKey in showInstanceKeys)
                        notifyInstance(notificationInstances.getValue(showInstanceKey), silent)

                    notificationInstances.values
                        .filter { !showInstanceKeys.contains(it.instanceKey) }
                        .forEach(::updateInstance)
                }
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
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
        } else {
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
                if (wereMaxShown) {
                    Preferences.tickLog.logLineHour("hiding group")
                    NotificationWrapper.instance.cancelNotification(NotificationWrapperImpl.NOTIFICATION_ID_GROUP_NOT_SUMMARY)
                } else {
                    hideInstanceKeys.cancelNotifications()
                }
            }

            // hide everything first, then show.  If applicable, FILO summary
            when {
                notificationInstances.size > MAX_NOTIFICATIONS_Q -> {
                    //hide
                    if (!wereMaxShown) shownInstanceKeys.cancelNotifications() // else group was already shown

                    //show
                    showSummary()

                    Preferences.tickLog.logLineHour("showing group")
                    NotificationWrapper.instance.notifyGroup(notificationInstances.values, silent, now, false)
                }
                notificationInstances.isNotEmpty() -> {
                    //hide
                    hideGroupOrOld()

                    //show
                    showSummary()

                    for (showInstanceKey in showInstanceKeys) {
                        val instance = notificationInstances.getValue(showInstanceKey)
                        Preferences.tickLog.logLineHour("showing '" + instance.name + "'")
                        notifyInstance(instance, silent)
                    }

                    // instances to be updated
                    notificationInstances.values
                            .filter { !showInstanceKeys.contains(it.instanceKey) }
                            .forEach {
                                Preferences.tickLog.logLineHour("updating '${it.name}' ${it.instanceDateTime}")
                                updateInstance(it)
                            }
                }
                else -> {
                    check(notificationInstances.isEmpty())
                    check(showInstanceKeys.isEmpty())
                    check(shownInstanceKeys == hideInstanceKeys)

                    // hide
                    hideGroupOrOld()

                    Preferences.tickLog.logLineHour("hiding summary")
                    NotificationWrapper.instance.cancelNotification(NotificationWrapperImpl.NOTIFICATION_ID_GROUP)
                }
            }
        }

        notifyInstances(notificationDatas, now)

        if (!silent) Preferences.lastTick = now.long

        if (clear) {
            notificationWrapper.updateAlarm(null)
        } else {
            val nextAlarmInstance = domainFactory.getRootInstances(now.toOffset().plusOne(), null, now)
                .filter { it.isAssignedToMe(now, domainFactory.myUserFactory.user) }
                .firstOrNull()

            if (nextAlarmInstance != null) {
                val nextAlarmTimeStamp = nextAlarmInstance.instanceDateTime.timeStamp

                check(nextAlarmTimeStamp.toLocalExactTimeStamp() > now)

                notificationWrapper.updateAlarm(nextAlarmTimeStamp)

                Preferences.tickLog.logLineHour("next tick: $nextAlarmTimeStamp")
            }
        }

        if (params.tick) {
            setIrrelevant(now)

            domainFactory.run { localFactory.deleteInstanceShownRecords(getAllTasks().map { it.taskKey }.toSet()) }
        }
    }

    private sealed class NotificationData {

        data class Cancel(val instanceId: Int) : NotificationData()

        data class Notify(val instance: Instance, val silent: Boolean = true) : NotificationData()
    }

    private fun notifyInstances(notificationDatas: List<NotificationData>, now: ExactTimeStamp.Local) {
        notificationDatas.filterIsInstance<NotificationData.Cancel>().forEach {
            notificationWrapper.cancelNotification(it.instanceId)
        }

        val notifies = notificationDatas.filterIsInstance<NotificationData.Notify>()

        val projectGroups = notifies.groupBy { it.instance.getProject().projectKey }

        val (private, shared) = projectGroups.entries.partition { it.key is ProjectKey.Private }

        fun NotificationData.Notify.notify() =
            notificationWrapper.notifyInstance(domainFactory.deviceDbInfo, instance, silent, now)

        private.singleOrNull()
            ?.value
            ?.forEach { it.notify() }

        shared.forEach { (_, sharedNotifies) ->
            check(sharedNotifies.isNotEmpty())

            if (sharedNotifies.size == 1) {
                sharedNotifies.single().notify()
            } else {
                val instances = sharedNotifies.map { it.instance }
                val silent = sharedNotifies.all { it.silent }

                instances.forEach { NotificationWrapper.instance.cancelNotification(it.notificationId) }

                val project = instances.map { it.getProject() }
                    .distinct()
                    .single() as SharedProject

                NotificationWrapper.instance.notifyProject(project, instances, silent, now)
            }
        }
    }

    private fun setIrrelevant(now: ExactTimeStamp.Local) {
        @Suppress("ConstantConditionIf")
        if (false) {
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

            domainFactory.projectsFactory
                    .projects
                    .values
                    .forEach {
                        val results = Irrelevant.setIrrelevant(
                                userCustomTimeRelevances,
                                it,
                                exactTimeStamp,
                                false,
                        )

                        results.irrelevantExistingInstances
                                .sortedBy { it.scheduleDateTime }
                                .forEach { Log.e("asdf", "magic irrelevant instance: $it") }

                        results.irrelevantSchedules
                                .sortedBy { it.startExactTimeStamp }
                                .forEach {
                                    Log.e("asdf", "magic irrelevant schedule, schedule: $it, task: ${it.topLevelTask}")
                                }
                    }

            throw Exception("Irrelevant.setIrrelevant write prevented")
        }

        val instances = domainFactory.getRootInstances(null, now.toOffset().plusOne(), now).toList()

        val irrelevantInstanceShownRecords = domainFactory.localFactory
                .instanceShownRecords
                .toMutableList()
                .apply { removeAll(instances.map { it.getShown(domainFactory.localFactory) }) }

        irrelevantInstanceShownRecords.forEach { it.delete() }
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