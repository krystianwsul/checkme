package com.krystianwsul.checkme.domainmodel.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.gui.instances.ShowNotificationGroupActivity
import com.krystianwsul.checkme.notifications.*
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.utils.time.TimeStamp
import java.util.*

open class NotificationWrapperImpl : NotificationWrapper() {

    companion object {

        val alarmManager by lazy { MyApplication.instance.getSystemService(Context.ALARM_SERVICE) as AlarmManager }
    }

    protected val notificationManager by lazy { MyApplication.instance.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private val lastNotificationBeeps = mutableMapOf<InstanceKey, Long>()

    override fun cancelNotification(id: Int) {
        Preferences.logLineHour("NotificationManager.cancel $id")
        notificationManager.cancel(id)
    }

    override fun notifyInstance(instance: Instance, silent: Boolean, now: ExactTimeStamp) {
        val reallySilent = if (silent) {
            true
        } else {
            lastNotificationBeeps.values.max()
                    ?.takeIf { SystemClock.elapsedRealtime() - it < 5000 }
                    ?.let { true } ?: false
        }

        notifyInstanceHelper(instance, reallySilent, now)

        if (!reallySilent)
            lastNotificationBeeps[instance.instanceKey] = SystemClock.elapsedRealtime()
    }

    protected fun notifyInstanceHelper(instance: Instance, silent: Boolean, now: ExactTimeStamp) {
        val task = instance.task
        val notificationId = instance.notificationId

        val instanceKey = instance.instanceKey

        fun pendingService(intent: Intent) = PendingIntent.getService(MyApplication.instance, notificationId, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        val pendingContentIntent = PendingIntent.getActivity(MyApplication.instance, notificationId, ShowInstanceActivity.getNotificationIntent(MyApplication.instance, instanceKey, notificationId), PendingIntent.FLAG_CANCEL_CURRENT)

        val pendingDeleteIntent = pendingService(InstanceNotificationDeleteService.getIntent(MyApplication.instance, instanceKey))

        val pendingDoneIntent = pendingService(InstanceDoneService.getIntent(MyApplication.instance, instanceKey, notificationId, instance.name))
        val pendingHourIntent = pendingService(InstanceHourService.getIntent(MyApplication.instance, instanceKey, notificationId, instance.name))

        fun action(@DrawableRes icon: Int, @StringRes text: Int, pendingIntent: PendingIntent) = NotificationCompat.Action
                .Builder(icon, MyApplication.instance.getString(text), pendingIntent)
                .build()

        val actions = listOf(
                action(R.drawable.ic_done_white_24dp, R.string.done, pendingDoneIntent),
                action(R.drawable.ic_alarm_white_24dp, R.string.hour, pendingHourIntent)
        )

        val childNames = getChildNames(instance, now)

        val text: String?
        val style: NotificationCompat.Style?
        if (childNames.isNotEmpty()) {
            text = childNames.joinToString(", ")
            style = getInboxStyle(childNames, false)
        } else if (!task.note.isNullOrEmpty()) {
            text = task.note
            style = NotificationCompat.BigTextStyle().also { it.bigText(text) }
        } else {
            val bigPicture = ImageManager.getBigPicture(task)
            if (bigPicture != null) {
                text = null
                style = NotificationCompat.BigPictureStyle().also { it.bigPicture(bigPicture) }
            } else {
                text = null
                style = null
            }
        }

        val timeStampLong = instance.instanceDateTime
                .timeStamp
                .long

        notify(
                instance.name,
                text,
                notificationId,
                pendingDeleteIntent,
                pendingContentIntent,
                silent,
                actions,
                timeStampLong,
                style,
                autoCancel = true,
                summary = false,
                sortKey = timeStampLong.toString() + instance.task
                        .startExactTimeStamp
                        .toString(),
                largeIcon = ImageManager.getLargeIcon(instance.task))
    }

    private fun getInstanceText(instance: Instance, now: ExactTimeStamp): String {
        val childNames = getChildNames(instance, now)

        return if (childNames.isNotEmpty()) {
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
        check(lines.isNotEmpty())

        val max = 5

        val inboxStyle = NotificationCompat.InboxStyle()

        lines.take(max).forEach { inboxStyle.addLine(it) }

        val extraCount = lines.size - max

        if (extraCount > 0)
            inboxStyle.setSummaryText("+" + extraCount + " " + MyApplication.instance.getString(R.string.more))

        return inboxStyle
    }

    @Suppress("DEPRECATION")
    protected open fun newBuilder(silent: Boolean) = NotificationCompat.Builder(MyApplication.instance)

    protected open fun getNotificationBuilder(
            title: String,
            text: String?,
            deleteIntent: PendingIntent,
            contentIntent: PendingIntent,
            silent: Boolean,
            actions: List<NotificationCompat.Action>,
            time: Long?,
            style: NotificationCompat.Style?,
            autoCancel: Boolean,
            summary: Boolean,
            sortKey: String,
            largeIcon: Bitmap?): NotificationCompat.Builder {
        check(title.isNotEmpty())

        val builder = newBuilder(silent)
                .setContentTitle(title)
                .setSmallIcon(R.drawable.ikona_bez)
                .setDeleteIntent(deleteIntent)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSortKey(sortKey)
                .setOnlyAlertOnce(true)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)

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

        largeIcon?.let { builder.setLargeIcon(it) }

        return builder
    }

    protected open fun notify(
            title: String,
            text: String?,
            notificationId: Int,
            deleteIntent: PendingIntent,
            contentIntent: PendingIntent,
            silent: Boolean,
            actions: List<NotificationCompat.Action>,
            time: Long?,
            style: NotificationCompat.Style?,
            autoCancel: Boolean,
            summary: Boolean,
            sortKey: String,
            largeIcon: Bitmap?) {
        val notification = getNotificationBuilder(
                title,
                text,
                deleteIntent,
                contentIntent,
                silent,
                actions,
                time,
                style,
                autoCancel,
                summary,
                sortKey,
                largeIcon).build()

        @Suppress("Deprecation")
        if (!silent)
            notification.defaults = notification.defaults or Notification.DEFAULT_VIBRATE

        MyCrashlytics.log("NotificationManager.notify $notificationId silent? $silent")
        notificationManager.notify(notificationId, notification)
    }

    override fun notifyGroup(instances: Collection<Instance>, silent: Boolean /* not needed >= 24 */, now: ExactTimeStamp) {
        val names = ArrayList<String>()
        val instanceKeys = ArrayList<InstanceKey>()
        instances.forEach {
            names.add(it.name)
            instanceKeys.add(it.instanceKey)
        }

        val deleteIntent = GroupNotificationDeleteService.getIntent(MyApplication.instance, instanceKeys)
        val pendingDeleteIntent = PendingIntent.getService(MyApplication.instance, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val contentIntent = ShowNotificationGroupActivity.getIntent(MyApplication.instance, instanceKeys)
        val pendingContentIntent = PendingIntent.getActivity(MyApplication.instance, 0, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val inboxStyle = getInboxStyle(instances
                .sortedWith(compareBy({ it.instanceDateTime.timeStamp }, { it.task.startExactTimeStamp }))
                .map { it.name + getInstanceText(it, now) }, true)

        notify(
                instances.size.toString() + " " + MyApplication.instance.getString(R.string.multiple_reminders),
                names.joinToString(", "),
                0,
                pendingDeleteIntent,
                pendingContentIntent,
                silent,
                listOf(),
                null,
                inboxStyle,
                autoCancel = false,
                summary = true,
                sortKey = "0",
                largeIcon = null)
    }

    override fun cleanGroup(lastNotificationId: Int?) {
        check(Build.VERSION.SDK_INT < Build.VERSION_CODES.N)

        if (lastNotificationId != null)
            cancelNotification(lastNotificationId)
    }

    override fun updateAlarm(nextAlarm: TimeStamp?) {
        val pendingIntent = PendingIntent.getBroadcast(MyApplication.instance, 0, AlarmReceiver.newIntent(), PendingIntent.FLAG_UPDATE_CURRENT)!!
        alarmManager.cancel(pendingIntent)
        nextAlarm?.let { setExact(it.long, pendingIntent) }
    }

    protected open fun setExact(time: Long, pendingIntent: PendingIntent) {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
    }

    override fun logNotificationIds(source: String) = Unit
}