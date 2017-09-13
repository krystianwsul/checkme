package com.krystianwsul.checkme.domainmodel

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.support.v4.app.NotificationCompat
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
import junit.framework.Assert
import java.util.*

open class NotificationWrapperImpl : NotificationWrapper() {

    override fun cancelNotification(context: Context, id: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        MyCrashlytics.log("NotificationManager.cancel " + id)
        notificationManager.cancel(id)
    }

    private fun getRemoteCustomTimeFixInstanceKey(domainFactory: DomainFactory, instance: Instance): InstanceKey { // remote custom time key hack
        val instanceKey = instance.instanceKey

        if (instanceKey.type == TaskKey.Type.LOCAL)
            return instanceKey

        if (instanceKey.mScheduleKey.ScheduleTimePair.mCustomTimeKey == null)
            return instanceKey

        if (instanceKey.mScheduleKey.ScheduleTimePair.mCustomTimeKey.type == TaskKey.Type.REMOTE)
            return instanceKey

        val projectId = instance.task.remoteNonNullProject.id

        val customTimeId = domainFactory.getRemoteCustomTimeId(projectId, instanceKey.mScheduleKey.ScheduleTimePair.mCustomTimeKey)

        val customTimeKey = CustomTimeKey(projectId, customTimeId)
        val scheduleKey = ScheduleKey(instanceKey.mScheduleKey.ScheduleDate, TimePair(customTimeKey))
        return InstanceKey(instanceKey.mTaskKey, scheduleKey)
    }

    override fun notifyInstance(context: Context, domainFactory: DomainFactory, instance: Instance, silent: Boolean, now: ExactTimeStamp, nougat: Boolean) {
        val task = instance.task
        val notificationId = instance.notificationId

        val instanceKey = instance.instanceKey
        val remoteCustomTimeFixInstanceKey = getRemoteCustomTimeFixInstanceKey(domainFactory, instance)

        val deleteIntent = InstanceNotificationDeleteService.getIntent(context, remoteCustomTimeFixInstanceKey)
        val pendingDeleteIntent = PendingIntent.getService(context, notificationId, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val contentIntent = ShowInstanceActivity.getNotificationIntent(context, instanceKey)
        val pendingContentIntent = PendingIntent.getActivity(context, notificationId, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val actions = ArrayList<NotificationCompat.Action>()

        val doneIntent = InstanceDoneService.getIntent(context, instanceKey, notificationId)
        val pendingDoneIntent = PendingIntent.getService(context, notificationId, doneIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        actions.add(NotificationCompat.Action.Builder(R.drawable.ic_done_white_24dp, context.getString(R.string.done), pendingDoneIntent).build())

        val hourIntent = InstanceHourService.getIntent(context, instanceKey, notificationId)
        val pendingHourIntent = PendingIntent.getService(context, notificationId, hourIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        actions.add(NotificationCompat.Action.Builder(R.drawable.ic_alarm_white_24dp, context.getString(R.string.hour), pendingHourIntent).build())

        val childNames = getChildNames(instance, now)

        val text: String?
        val style: NotificationCompat.Style?
        if (!childNames.isEmpty()) {
            text = childNames.joinToString(", ")
            style = getInboxStyle(context, childNames, false)
        } else if (!task.note.isNullOrEmpty()) {
            text = task.note

            val bigTextStyle = NotificationCompat.BigTextStyle()
            bigTextStyle.bigText(task.note)

            style = bigTextStyle
        } else {
            text = null
            style = null
        }

        notify(context, instance.name, text, notificationId, pendingDeleteIntent, pendingContentIntent, silent, actions, instance.instanceDateTime.timeStamp.long, style, true, nougat, false, instance.instanceDateTime.timeStamp.long!!.toString() + instance.task.startExactTimeStamp.toString())
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

    private fun getChildNames(instance: Instance, now: ExactTimeStamp): List<String> {
        val childInstances = instance.getChildInstances(now)

        val notDone = childInstances.filter { it.done == null }.sortedBy { it.task.startExactTimeStamp }

        val done = childInstances.filter { it.done != null }.sortedBy { it.done!!.long }

        return (notDone + done).map(Instance::getName)
    }

    private fun getInboxStyle(context: Context, lines: List<String>, nougatGroup: Boolean): NotificationCompat.InboxStyle {
        Assert.assertTrue(!lines.isEmpty())

        val max = 5

        val inboxStyle = NotificationCompat.InboxStyle()

        lines.take(max).forEach { inboxStyle.addLine(it) }

        val extraCount = lines.size - max

        if (extraCount > 0 && !nougatGroup)
            inboxStyle.setSummaryText("+" + extraCount + " " + context.getString(R.string.more))

        return inboxStyle
    }

    private fun notify(context: Context, title: String, text: String?, notificationId: Int, deleteIntent: PendingIntent, contentIntent: PendingIntent, silent: Boolean, actions: List<NotificationCompat.Action>, time: Long?, style: NotificationCompat.Style?, autoCancel: Boolean, nougat: Boolean, summary: Boolean, sortKey: String) {
        Assert.assertTrue(title.isNotEmpty())

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(context)
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

        Assert.assertTrue(actions.size <= 3)

        actions.forEach { builder.addAction(it) }

        if (time != null)
            builder.setWhen(time).setShowWhen(true)

        if (style != null)
            builder.setStyle(style)

        if (autoCancel)
            builder.setAutoCancel(true)

        if (nougat) {
            builder.setGroup(TickService.GROUP_KEY)

            if (summary)
                builder.setGroupSummary(true)
        }

        val notification = builder.build()

        if (!silent)
            notification.defaults = notification.defaults or Notification.DEFAULT_VIBRATE

        MyCrashlytics.log("NotificationManager.notify " + notificationId)
        notificationManager.notify(notificationId, notification)
    }

    @SuppressLint("NewApi")
    protected open fun setExact(context: Context, time: Long, pendingIntent: PendingIntent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent)
    }

    override fun notifyGroup(context: Context, domainFactory: DomainFactory, instances: Collection<Instance>, silent: Boolean, now: ExactTimeStamp, nougat: Boolean) {
        val names = ArrayList<String>()
        val instanceKeys = ArrayList<InstanceKey>()
        val remoteCustomTimeFixInstanceKeys = ArrayList<InstanceKey>()
        instances.forEach {
            names.add(it.name)
            instanceKeys.add(it.instanceKey)
            remoteCustomTimeFixInstanceKeys.add(getRemoteCustomTimeFixInstanceKey(domainFactory, it))
        }

        val deleteIntent = GroupNotificationDeleteService.getIntent(context, remoteCustomTimeFixInstanceKeys)
        val pendingDeleteIntent = PendingIntent.getService(context, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val contentIntent = ShowNotificationGroupActivity.getIntent(context, instanceKeys)
        val pendingContentIntent = PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val inboxStyle = getInboxStyle(context, instances
                .sortedWith(compareBy({ it.instanceDateTime.timeStamp }, { it.task.startExactTimeStamp }))
                .map { it.name + getInstanceText(it, now) }, nougat)

        notify(context, instances.size.toString() + " " + context.getString(R.string.multiple_reminders), names.joinToString(", "), 0, pendingDeleteIntent, pendingContentIntent, silent, ArrayList(), null, inboxStyle, false, nougat, true, "0")
    }

    override fun setAlarm(context: Context, pendingIntent: PendingIntent, nextAlarm: TimeStamp) {
        setExact(context, nextAlarm.long!!, pendingIntent)
    }

    override fun getPendingIntent(context: Context): PendingIntent {
        val nextIntent = TickService.getIntent(context, false, "NotificationWrapper: TickService.getIntent")

        val pendingIntent = PendingIntent.getService(context, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        Assert.assertTrue(pendingIntent != null)

        return pendingIntent
    }

    override fun cancelAlarm(context: Context, pendingIntent: PendingIntent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.cancel(pendingIntent)
    }

    override fun cleanGroup(context: Context, lastNotificationId: Int?) {
        Assert.assertTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.N)

        if (lastNotificationId != null)
            cancelNotification(context, lastNotificationId)
    }
}