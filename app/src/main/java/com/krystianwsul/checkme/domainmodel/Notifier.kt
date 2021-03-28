package com.krystianwsul.checkme.domainmodel

import android.os.Build
import android.util.Log
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapperImpl
import com.krystianwsul.checkme.ticks.Ticker
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Project
import com.krystianwsul.common.relevance.Irrelevant
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import com.soywiz.klock.DateTime
import com.soywiz.klock.Time
import com.soywiz.klock.days
import com.soywiz.klock.hours

class Notifier(private val domainFactory: DomainFactory, private val notificationWrapper: NotificationWrapper) {

    companion object {

        private const val MAX_NOTIFICATIONS_OLD = 3

        // To prevent spam if there's a huge backlog
        private const val MAX_NOTIFICATIONS_Q = 10
    }

    fun updateNotificationsTick(
            now: ExactTimeStamp.Local,
            silent: Boolean,
            sourceName: String,
            domainChanged: Boolean = false,
    ) {
        updateNotifications(now, silent = silent, sourceName = sourceName, domainChanged = domainChanged)

        setIrrelevant(now)

        domainFactory.projectsFactory.let { domainFactory.localFactory.deleteInstanceShownRecords(it.taskKeys) }
    }

    private fun setIrrelevant(now: ExactTimeStamp.Local) {
        @Suppress("ConstantConditionIf")
        if (false) {
            val tomorrow = (DateTimeSoy.now() + 1.days).date
            val dateTimeSoy = DateTime(tomorrow, Time(2.hours))
            val exactTimeStamp = ExactTimeStamp.Local(dateTimeSoy)

            domainFactory.projectsFactory
                    .projects
                    .values
                    .forEach {
                        val results = Irrelevant.setIrrelevant(
                                object : Project.Parent {

                                    override fun deleteProject(project: Project<*>) = throw NotImplementedError()
                                },
                                it,
                                exactTimeStamp,
                                false
                        )

                        results.irrelevantExistingInstances
                                .sortedBy { it.scheduleDateTime }
                                .forEach { Log.e("asdf", "magic irrelevant instance: $it") }

                        results.irrelevantSchedules
                                .sortedBy { it.startExactTimeStamp }
                                .forEach {
                                    Log.e("asdf", "magic irrelevant schedule, schedule: $it, task: ${it.rootTask}")
                                }
                    }

            throw Exception("Irrelevant.setIrrelevant write prevented")
        }

        val instances = domainFactory.projectsFactory.projects
                .values
                .map {
                    it.existingInstances + it.getRootInstances(null, now.toOffset().plusOne(), now)
                }
                .flatten()

        val irrelevantInstanceShownRecords = domainFactory.localFactory
                .instanceShownRecords
                .toMutableList()
                .apply { removeAll(instances.map { it.getShown(domainFactory.localFactory) }) }

        irrelevantInstanceShownRecords.forEach { it.delete() }
    }

    fun updateNotifications(
            now: ExactTimeStamp.Local,
            clear: Boolean = false,
            silent: Boolean = true,
            removedTaskKeys: List<TaskKey> = listOf(),
            sourceName: String = "other",
            domainChanged: Boolean = false,
    ) {
        val skipSave = domainFactory.aggregateData != null

        Preferences.tickLog.logLineDate("updateNotifications start $sourceName, skipping? $skipSave")

        if (skipSave) {
            TickHolder.addTickData(TickData.Normal(silent, sourceName, domainChanged))
            return
        }

        notificationWrapper.hideTemporary(Ticker.TICK_NOTIFICATION_ID, sourceName)

        val notificationInstances = if (clear) {
            mapOf()
        } else {
            domainFactory.getRootInstances(null, now.toOffset().plusOne(), now /* 24 hack */)
                    .filter {
                        it.done == null
                                && !it.getNotified(domainFactory.localFactory)
                                && it.instanceDateTime.toLocalExactTimeStamp() <= now
                                && !removedTaskKeys.contains(it.taskKey)
                                && it.isAssignedToMe(now, domainFactory.myUserFactory.user)
                    }
                    .associateBy { it.instanceKey }
        }

        Preferences.tickLog.logLineHour(
                "notification instances: " + notificationInstances.values.joinToString(", ") { it.name }
        )

        val instanceShownPairs = domainFactory.localFactory.instanceShownRecords
                .filter { it.notificationShown }
                .map {
                    it to domainFactory.projectsFactory.getProjectIfPresent(it.projectId)?.getTaskIfPresent(it.taskId)
                }

        instanceShownPairs.filter { it.second == null }.forEach { (instanceShownRecord, _) ->
            val scheduleDate = instanceShownRecord.run { Date(scheduleYear, scheduleMonth, scheduleDay) }
            val customTimeId = instanceShownRecord.scheduleCustomTimeId

            val customTimePair: Pair<String, String>?
            val hourMinute: HourMinute?
            if (!customTimeId.isNullOrEmpty()) {
                check(instanceShownRecord.scheduleHour == null)
                check(instanceShownRecord.scheduleMinute == null)

                customTimePair = Pair(instanceShownRecord.projectId, customTimeId)
                hourMinute = null
            } else {
                checkNotNull(instanceShownRecord.scheduleHour)
                checkNotNull(instanceShownRecord.scheduleMinute)

                customTimePair = null
                hourMinute = instanceShownRecord.run { HourMinute(scheduleHour!!, scheduleMinute!!) }
            }

            val taskKey = Pair(instanceShownRecord.projectId, instanceShownRecord.taskId)

            NotificationWrapper.instance.cancelNotification(
                    Instance.getNotificationId(
                            scheduleDate,
                            customTimePair,
                            hourMinute,
                            taskKey
                    )
            )
            instanceShownRecord.notificationShown = false
        }

        val shownInstanceKeys = instanceShownPairs.filter { it.second != null }
                .map { (instanceShownRecord, task) ->
                    val scheduleDate = instanceShownRecord.run { Date(scheduleYear, scheduleMonth, scheduleDay) }
                    val customTimeId = instanceShownRecord.scheduleCustomTimeId
                    val project = task!!.project

                    val customTimeKey: CustomTimeKey<*>?
                    val hourMinute: HourMinute?
                    if (!customTimeId.isNullOrEmpty()) {
                        check(instanceShownRecord.scheduleHour == null)
                        check(instanceShownRecord.scheduleMinute == null)

                        customTimeKey = project.getCustomTime(customTimeId).key
                        hourMinute = null
                    } else {
                        checkNotNull(instanceShownRecord.scheduleHour)
                        checkNotNull(instanceShownRecord.scheduleMinute)

                        customTimeKey = null
                        hourMinute = instanceShownRecord.run { HourMinute(scheduleHour!!, scheduleMinute!!) }
                    }

                    val taskKey = TaskKey(project.projectKey, instanceShownRecord.taskId)
                    InstanceKey(taskKey, scheduleDate, TimePair(customTimeKey, hourMinute))
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
                        notificationWrapper.cancelNotification(
                                domainFactory.getInstance(shownInstanceKey).notificationId
                        )
                    }

                    notificationWrapper.notifyGroup(notificationInstances.values, silent, now)
                }
            } else { // show instances
                if (shownInstanceKeys.size > MAX_NOTIFICATIONS_OLD) { // group shown
                    NotificationWrapper.instance.cancelNotification(0)

                    for (instance in notificationInstances.values)
                        notifyInstance(instance, silent, now)
                } else { // instances shown
                    for (hideInstanceKey in hideInstanceKeys) {
                        notificationWrapper.cancelNotification(
                                domainFactory.getInstance(hideInstanceKey).notificationId
                        )
                    }

                    for (showInstanceKey in showInstanceKeys)
                        notifyInstance(notificationInstances.getValue(showInstanceKey), silent, now)

                    notificationInstances.values
                            .filter { !showInstanceKeys.contains(it.instanceKey) }
                            .forEach { updateInstance(it, now) }
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
                NotificationWrapper.instance.cancelNotification(instance.notificationId)
            }

            for (showInstanceKey in showInstanceKeys) {
                val instance = notificationInstances.getValue(showInstanceKey)
                Preferences.tickLog.logLineHour("showing '" + instance.name + "'")
                notifyInstance(instance, silent, now)
            }

            val updateInstances = notificationInstances.values.filter { !showInstanceKeys.contains(it.instanceKey) }

            updateInstances.forEach {
                Preferences.tickLog.logLineHour("updating '" + it.name + "' " + it.instanceDateTime)
                updateInstance(it, now)
            }
        } else {
            /**
             * in this section, "summary" is Android's summary notification thingy, whereas "group" is my own
             * inbox-style notification
             */

            fun Collection<InstanceKey>.cancelNotifications() = map(domainFactory::getInstance).forEach {
                Preferences.tickLog.logLineHour("hiding '" + it.name + "'")
                NotificationWrapper.instance.cancelNotification(it.notificationId)
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
                        notifyInstance(instance, silent, now)
                    }

                    // instances to be updated
                    notificationInstances.values
                            .filter { !showInstanceKeys.contains(it.instanceKey) }
                            .forEach {
                                Preferences.tickLog.logLineHour("updating '${it.name}' ${it.instanceDateTime}")
                                updateInstance(it, now)
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

        if (!silent) Preferences.lastTick = now.long

        val nextAlarm = domainFactory.getTasks()
                .filter { it.current(now) && it.isRootTask(now) }
                .mapNotNull { it.getNextAlarm(now, domainFactory.myUserFactory.user) }
                .minOrNull()
                .takeUnless { clear }

        notificationWrapper.updateAlarm(nextAlarm)

        nextAlarm?.let { Preferences.tickLog.logLineHour("next tick: $it") }
    }

    private fun notifyInstance(instance: Instance<*>, silent: Boolean, now: ExactTimeStamp.Local) =
            notificationWrapper.notifyInstance(domainFactory.deviceDbInfo, instance, silent, now)

    private fun updateInstance(instance: Instance<*>, now: ExactTimeStamp.Local) =
            notificationWrapper.notifyInstance(domainFactory.deviceDbInfo, instance, true, now)
}