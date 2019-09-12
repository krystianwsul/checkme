package com.krystianwsul.checkme.domainmodel.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.gui.instances.ShowNotificationGroupActivity
import com.krystianwsul.checkme.notifications.*
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.Instance
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import io.reactivex.BackpressureStrategy
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.*

open class NotificationWrapperImpl : NotificationWrapper() {

    companion object {

        val alarmManager by lazy { MyApplication.instance.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

        @JvmStatic
        protected val KEY_HASH_CODE = "com.krystianwsul.checkme.notification_hash_code"

        @JvmStatic
        protected val MAX_INBOX_LINES = 5
    }

    protected val notificationManager by lazy { MyApplication.instance.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private val lastNotificationBeeps = mutableMapOf<InstanceKey, Long>()

    private val notificationRelay = PublishRelay.create<() -> Unit>()

    init {
        notificationRelay.toFlowable(BackpressureStrategy.BUFFER)
                .observeOn(Schedulers.io())
                .flatMapSingle({ Single.fromCallable(it) }, false, 1)
                .subscribe()
    }

    override fun cancelNotification(id: Int) {
        Preferences.logLineHour("NotificationManager.cancel $id")
        notificationManager.cancel(id)
    }

    final override fun notifyInstance(deviceDbInfo: DeviceDbInfo, instance: Instance, silent: Boolean, now: ExactTimeStamp) { // consider queueing all notifications and group in a batch
        val instanceData = getInstanceData(deviceDbInfo, instance, silent, now)
        notificationRelay.accept { notifyInstanceHelper(instanceData) }
    }

    protected open fun getInstanceData(deviceDbInfo: DeviceDbInfo, instance: Instance, silent: Boolean, now: ExactTimeStamp): InstanceData {
        val reallySilent = if (silent) {
            true
        } else {
            lastNotificationBeeps.values.max()
                    ?.takeIf { SystemClock.elapsedRealtime() - it < 5000 }
                    ?.let { true } ?: false
        }

        if (!reallySilent)
            lastNotificationBeeps[instance.instanceKey] = SystemClock.elapsedRealtime()

        return InstanceData(deviceDbInfo, instance, now, reallySilent)
    }

    private fun notifyInstanceHelper(instanceData: InstanceData) {
        val notificationId = instanceData.notificationId

        val instanceKey = instanceData.instanceKey

        fun pendingService(intent: Intent) = PendingIntent.getService(MyApplication.instance, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val pendingContentIntent = PendingIntent.getActivity(MyApplication.instance, notificationId, ShowInstanceActivity.getNotificationIntent(MyApplication.instance, instanceKey, notificationId), PendingIntent.FLAG_UPDATE_CURRENT)

        val pendingDeleteIntent = pendingService(InstanceNotificationDeleteService.getIntent(MyApplication.instance, instanceKey))

        val pendingDoneIntent = pendingService(InstanceDoneService.getIntent(MyApplication.instance, instanceKey, notificationId, instanceData.name))
        val pendingHourIntent = pendingService(InstanceHourService.getIntent(MyApplication.instance, instanceKey, notificationId, instanceData.name))

        fun action(@DrawableRes icon: Int, @StringRes text: Int, pendingIntent: PendingIntent) = NotificationCompat.Action
                .Builder(icon, MyApplication.instance.getString(text), pendingIntent)
                .build()

        val actions = listOf(
                action(R.drawable.ic_done_white_24dp, R.string.done, pendingDoneIntent),
                action(R.drawable.ic_alarm_white_24dp, R.string.hour, pendingHourIntent)
        )

        val childNames = instanceData.childNames

        val text: String?
        val style: (() -> NotificationCompat.Style)?
        val styleHash: NotificationHash.Style?
        if (childNames.isNotEmpty()) {
            text = childNames.joinToString(", ")

            val pair = getInboxStyle(childNames, false)
            style = pair.first
            styleHash = pair.second
        } else if (!instanceData.note.isNullOrEmpty()) {
            text = instanceData.note
            style = {
                NotificationCompat.BigTextStyle().also { it.bigText(text) }
            }
            styleHash = NotificationHash.Style.Text(text)
        } else {
            val bigPicture = ImageManager.getBigPicture(instanceData.uuid)
            if (bigPicture != null) {
                text = null
                style = {
                    NotificationCompat.BigPictureStyle().also {
                        it.bigPicture(bigPicture())
                        it.bigLargeIcon(null)
                    }
                }
                styleHash = NotificationHash.Style.Picture(instanceData.uuid!!)
            } else {
                text = null
                style = null
                styleHash = null
            }
        }

        val timeStampLong = instanceData.timeStampLong

        val sortKey = timeStampLong.toString() + instanceData.startExactTimeStamp

        val largeIcon = ImageManager.getLargeIcon(instanceData.uuid)

        val notificationHash = NotificationHash(
                instanceData.name,
                text,
                notificationId,
                timeStampLong,
                styleHash,
                sortKey,
                largeIcon?.let { instanceData.uuid!! }
        )

        notify(
                instanceData.name,
                text,
                notificationId,
                pendingDeleteIntent,
                pendingContentIntent,
                instanceData.silent,
                actions,
                timeStampLong,
                style,
                autoCancel = true,
                summary = false,
                sortKey = sortKey,
                largeIcon = largeIcon,
                notificationHash = notificationHash)
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

    protected open fun getExtraCount(lines: List<String>, group: Boolean) = lines.size - MAX_INBOX_LINES

    private fun getInboxStyle(lines: List<String>, group: Boolean): Pair<() -> NotificationCompat.InboxStyle, NotificationHash.Style.Inbox> {
        check(lines.isNotEmpty())

        val inboxStyle = NotificationCompat.InboxStyle()

        val finalLines = lines.take(MAX_INBOX_LINES)

        finalLines.forEach { inboxStyle.addLine(it) }

        val extraCount = getExtraCount(lines, group)

        if (extraCount > 0)
            inboxStyle.setSummaryText("+" + extraCount + " " + MyApplication.instance.getString(R.string.more))

        return Pair({ inboxStyle }, NotificationHash.Style.Inbox(finalLines, extraCount))
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
            style: (() -> NotificationCompat.Style)?,
            autoCancel: Boolean,
            summary: Boolean,
            sortKey: String,
            largeIcon: (() -> Bitmap)?,
            notificationHash: NotificationHash): NotificationCompat.Builder {
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
                .addExtras(Bundle().apply { putInt(KEY_HASH_CODE, notificationHash.hashCode()) })

        if (!text.isNullOrEmpty())
            builder.setContentText(text)

        if (!silent)
            builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)

        check(actions.size <= 3)

        actions.forEach { builder.addAction(it) }

        if (time != null)
            builder.setWhen(time).setShowWhen(true)

        if (style != null)
            builder.setStyle(style())

        if (autoCancel)
            builder.setAutoCancel(true)

        largeIcon?.let { builder.setLargeIcon(it()) }

        return builder
    }

    protected open fun unchanged(notificationHash: NotificationHash) = false

    protected open fun notify(
            title: String,
            text: String?,
            notificationId: Int,
            deleteIntent: PendingIntent,
            contentIntent: PendingIntent,
            silent: Boolean,
            actions: List<NotificationCompat.Action>,
            time: Long?,
            style: (() -> NotificationCompat.Style)?,
            autoCancel: Boolean,
            summary: Boolean,
            sortKey: String,
            largeIcon: (() -> Bitmap)?,
            notificationHash: NotificationHash) {
        if (unchanged(notificationHash)) {
            Preferences.logLineHour("skipping notification update for $title")
            return
        }

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
                largeIcon,
                notificationHash).build()

        @Suppress("Deprecation")
        if (!silent)
            notification.defaults = notification.defaults or Notification.DEFAULT_VIBRATE

        MyCrashlytics.log("NotificationManager.notify $notificationId silent? $silent")
        notificationManager.notify(notificationId, notification)
    }

    override fun notifyGroup(instances: Collection<Instance>, silent: Boolean /* not needed >= 24 */, now: ExactTimeStamp) {
        val groupData = GroupData(instances, now, silent)
        notificationRelay.accept { notifyGroupHelper(groupData) }
    }

    private inner class GroupData(
            instances: Collection<com.krystianwsul.common.domain.Instance>,
            private val now: ExactTimeStamp,
            val silent: Boolean) {

        val instances = instances.map(::Instance)

        inner class Instance(instance: com.krystianwsul.common.domain.Instance) {

            val name = instance.name
            val instanceKey = instance.instanceKey
            val timeStamp = instance.instanceDateTime.timeStamp
            val startExactTimeStamp = instance.task.startExactTimeStamp
            val text = getInstanceText(instance, now)
        }
    }

    private fun notifyGroupHelper(groupData: GroupData) {
        val names = ArrayList<String>()
        val instanceKeys = ArrayList<InstanceKey>()
        groupData.instances.forEach {
            names.add(it.name)
            instanceKeys.add(it.instanceKey)
        }

        val deleteIntent = GroupNotificationDeleteService.getIntent(MyApplication.instance, instanceKeys)
        val pendingDeleteIntent = PendingIntent.getService(MyApplication.instance, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val contentIntent = ShowNotificationGroupActivity.getIntent(MyApplication.instance, instanceKeys)
        val pendingContentIntent = PendingIntent.getActivity(MyApplication.instance, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val (inboxStyle, styleHash) = getInboxStyle(groupData.instances
                .sortedWith(compareBy({ it.timeStamp }, { it.startExactTimeStamp }))
                .map { it.name + it.text }, true)

        val title = groupData.instances.size.toString() + " " + MyApplication.instance.getString(R.string.multiple_reminders)
        val text = names.joinToString(", ")

        val notificationHash = NotificationHash(
                title,
                text,
                0,
                null,
                styleHash,
                "0",
                null)

        notify(
                title,
                text,
                0,
                pendingDeleteIntent,
                pendingContentIntent,
                groupData.silent,
                listOf(),
                null,
                inboxStyle,
                autoCancel = false,
                summary = true,
                sortKey = "0",
                largeIcon = null,
                notificationHash = notificationHash)
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

    protected data class NotificationHash(
            val title: String,
            val text: String?,
            val id: Int,
            val timeStamp: Long?,
            val style: Style?,
            val sortKey: String,
            val uuid: String?) {

        interface Style {

            data class Inbox(val lines: List<String>, val extraCount: Int) : Style
            data class Text(val text: String?) : Style
            data class Picture(val uuid: String) : Style
        }
    }

    protected inner class InstanceData(deviceDbInfo: DeviceDbInfo, instance: Instance, now: ExactTimeStamp, val silent: Boolean) {

        val notificationId = instance.notificationId

        val instanceKey = instance.instanceKey

        val note = instance.task.note

        val uuid = instance.task
                .getImage(deviceDbInfo)
                ?.uuid

        val timeStampLong = instance.instanceDateTime
                .timeStamp
                .long

        val startExactTimeStamp = instance.task
                .startExactTimeStamp
                .toString()

        val name = instance.name

        val childNames = getChildNames(instance, now)
    }
}