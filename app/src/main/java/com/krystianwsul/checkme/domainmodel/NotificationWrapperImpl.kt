package com.krystianwsul.checkme.domainmodel

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.firebase.jobdispatcher.*
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.gui.instances.ShowNotificationGroupActivity
import com.krystianwsul.checkme.notifications.*
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.ScheduleKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.utils.time.TimePair
import com.krystianwsul.checkme.utils.time.TimeStamp
import java.util.*

open class NotificationWrapperImpl : NotificationWrapper() {

    companion object {

        fun getRemoteCustomTimeFixInstanceKey(domainFactory: DomainFactory, instanceKey: InstanceKey): InstanceKey { // remote custom time key hack
            if (instanceKey.type == TaskKey.Type.LOCAL)
                return instanceKey

            if (instanceKey.scheduleKey.scheduleTimePair.customTimeKey == null)
                return instanceKey

            if (instanceKey.scheduleKey.scheduleTimePair.customTimeKey is CustomTimeKey.RemoteCustomTimeKey)
                return instanceKey

            val projectId = instanceKey.taskKey.remoteProjectId!!

            val customTimeId = domainFactory.getRemoteCustomTimeId(projectId, instanceKey.scheduleKey.scheduleTimePair.customTimeKey)

            val customTimeKey = CustomTimeKey.RemoteCustomTimeKey(projectId, customTimeId)
            val scheduleKey = ScheduleKey(instanceKey.scheduleKey.scheduleDate, TimePair(customTimeKey))
            return InstanceKey(instanceKey.taskKey, scheduleKey)
        }

        val alarmManager by lazy { MyApplication.instance.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

        private const val TAG = "tickService"
    }

    protected val notificationManager by lazy { MyApplication.instance.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun cancelNotification(id: Int) {
        MyCrashlytics.log("NotificationManager.cancel $id")
        notificationManager.cancel(id)
    }

    override fun notifyInstance(domainFactory: DomainFactory, instance: Instance, silent: Boolean, now: ExactTimeStamp) {
        val task = instance.task
        val notificationId = instance.notificationId

        val instanceKey = instance.instanceKey
        val remoteCustomTimeFixInstanceKey = getRemoteCustomTimeFixInstanceKey(domainFactory, instanceKey)

        val deleteIntent = InstanceNotificationDeleteService.getIntent(MyApplication.instance, remoteCustomTimeFixInstanceKey)
        val pendingDeleteIntent = PendingIntent.getService(MyApplication.instance, notificationId, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val contentIntent = ShowInstanceActivity.getNotificationIntent(MyApplication.instance, instanceKey)
        val pendingContentIntent = PendingIntent.getActivity(MyApplication.instance, notificationId, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val actions = ArrayList<NotificationCompat.Action>()

        val doneIntent = InstanceDoneService.getIntent(MyApplication.instance, instanceKey, notificationId)
        val pendingDoneIntent = PendingIntent.getService(MyApplication.instance, notificationId, doneIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        actions.add(NotificationCompat.Action.Builder(R.drawable.ic_done_white_24dp, MyApplication.instance.getString(R.string.done), pendingDoneIntent).build())

        val hourIntent = InstanceHourService.getIntent(MyApplication.instance, instanceKey, notificationId)
        val pendingHourIntent = PendingIntent.getService(MyApplication.instance, notificationId, hourIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        actions.add(NotificationCompat.Action.Builder(R.drawable.ic_alarm_white_24dp, MyApplication.instance.getString(R.string.hour), pendingHourIntent).build())

        val childNames = getChildNames(instance, now)

        val text: String?
        val style: NotificationCompat.Style?
        if (!childNames.isEmpty()) {
            text = childNames.joinToString(", ")
            style = getInboxStyle(childNames, false)
        } else if (!task.note.isNullOrEmpty()) {
            text = task.note

            val bigTextStyle = NotificationCompat.BigTextStyle()
            bigTextStyle.bigText(task.note)

            style = bigTextStyle
        } else {
            text = null
            style = null
        }

        notify(instance.name, text, notificationId, pendingDeleteIntent, pendingContentIntent, silent, actions, instance.instanceDateTime.timeStamp.long, style, true, false, instance.instanceDateTime.timeStamp.long.toString() + instance.task.startExactTimeStamp.toString())
    }

    private fun getInstanceText(instance: Instance, now: ExactTimeStamp): String {
        val childNames = getChildNames(instance, now)

        return if (!childNames.isEmpty()) {
            " (" + childNames.joinToString(", ") + ")"
        } else {
            val note = instance.task.note

            if (!note.isNullOrEmpty()) " ($note)" else ""
        }
    }

    private fun getChildNames(instance: Instance, now: ExactTimeStamp) = instance.getChildInstances(now)
            .asSequence()
            .filter { it.first.done == null }
            .sortedBy { it.second.ordinal }
            .map { it.first.name }
            .toList()

    protected open fun getInboxStyle(lines: List<String>, group: Boolean): NotificationCompat.InboxStyle {
        check(!lines.isEmpty())

        val max = 5

        val inboxStyle = NotificationCompat.InboxStyle()

        lines.take(max).forEach { inboxStyle.addLine(it) }

        val extraCount = lines.size - max

        if (extraCount > 0)
            inboxStyle.setSummaryText("+" + extraCount + " " + MyApplication.instance.getString(R.string.more))

        return inboxStyle
    }

    protected open fun notify(title: String, text: String?, notificationId: Int, deleteIntent: PendingIntent, contentIntent: PendingIntent, silent: Boolean, actions: List<NotificationCompat.Action>, time: Long?, style: NotificationCompat.Style?, autoCancel: Boolean, summary: Boolean, sortKey: String) {
        check(title.isNotEmpty())

        @Suppress("DEPRECATION")
        val builder = NotificationCompat.Builder(MyApplication.instance)
                .setContentTitle(title)
                .setSmallIcon(R.drawable.ikona_bez)
                .setDeleteIntent(deleteIntent)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSortKey(sortKey) as NotificationCompat.Builder

        if (!text.isNullOrEmpty())
            builder.setContentText(text)

        if (!silent)
            builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)

        check(actions.size <= 3)

        actions.forEach { builder.addAction(it) }

        if (time != null)
            builder.setWhen(time).setShowWhen(true)

        if (style != null)
            builder.setStyle(style)

        if (autoCancel)
            builder.setAutoCancel(true)

        val notification = builder.build()

        @Suppress("Deprecation")
        if (!silent)
            notification.defaults = notification.defaults or Notification.DEFAULT_VIBRATE

        MyCrashlytics.log("NotificationManager.notify $notificationId")
        notificationManager.notify(notificationId, notification)
    }

    override fun notifyGroup(domainFactory: DomainFactory, instances: Collection<Instance>, silent: Boolean, now: ExactTimeStamp) {
        val names = ArrayList<String>()
        val instanceKeys = ArrayList<InstanceKey>()
        val remoteCustomTimeFixInstanceKeys = ArrayList<InstanceKey>()
        instances.forEach {
            names.add(it.name)
            instanceKeys.add(it.instanceKey)
            remoteCustomTimeFixInstanceKeys.add(getRemoteCustomTimeFixInstanceKey(domainFactory, it.instanceKey))
        }

        val deleteIntent = GroupNotificationDeleteService.getIntent(MyApplication.instance, remoteCustomTimeFixInstanceKeys)
        val pendingDeleteIntent = PendingIntent.getService(MyApplication.instance, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val contentIntent = ShowNotificationGroupActivity.getIntent(MyApplication.instance, instanceKeys)
        val pendingContentIntent = PendingIntent.getActivity(MyApplication.instance, 0, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val inboxStyle = getInboxStyle(instances
                .sortedWith(compareBy({ it.instanceDateTime.timeStamp }, { it.task.startExactTimeStamp }))
                .map { it.name + getInstanceText(it, now) }, true)

        notify(instances.size.toString() + " " + MyApplication.instance.getString(R.string.multiple_reminders), names.joinToString(", "), 0, pendingDeleteIntent, pendingContentIntent, silent, ArrayList(), null, inboxStyle, false, true, "0")
    }

    override fun cleanGroup(lastNotificationId: Int?) {
        check(Build.VERSION.SDK_INT < Build.VERSION_CODES.N)

        if (lastNotificationId != null)
            cancelNotification(lastNotificationId)
    }

    override fun updateAlarm(nextAlarm: TimeStamp?) {
        Log.e("asdf", "updateAlarm start")
        if (nextAlarm != null) {

            Log.e("asdf", "updateAlarm setExact")
            setExact(nextAlarm.long)

            val now = ExactTimeStamp.now
            val delay = ((nextAlarm.long - now.long) / 1000).toInt()

            Log.e("asdf", "updateAlarm dispatcher")
            FirebaseJobDispatcher(GooglePlayDriver(MyApplication.instance)).apply {
                Log.e("asdf", "updateAlarm job")
                val job = newJobBuilder().setService(FirebaseTickService::class.java)
                        .setTag(TAG)
                        .setRecurring(false)
                        .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                        .setTrigger(Trigger.executionWindow(delay, delay + 5))
                        .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                        .setReplaceCurrent(true)
                        .build()

                Log.e("asdf", "updateAlarm mustSchedule")
                mustSchedule(job)
            }
        }
        Log.e("asdf", "updateAlarm end")
    }

    protected open fun setExact(time: Long) {
        val pendingIntent = PendingIntent.getBroadcast(MyApplication.instance, 0, AlarmReceiver.newIntent("setExact"), PendingIntent.FLAG_UPDATE_CURRENT)!!
        alarmManager.cancel(pendingIntent)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
    }
}