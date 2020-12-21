package com.krystianwsul.checkme.domainmodel.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
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
import com.krystianwsul.checkme.gui.main.MainActivity
import com.krystianwsul.checkme.notifications.NotificationAction
import com.krystianwsul.checkme.notifications.NotificationActionReceiver
import com.krystianwsul.checkme.ticks.AlarmReceiver
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.models.Instance
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

        private const val NOTIFICATION_ID_GROUP = 0

        private const val TAG_TEMPORARY = "temporary"

        val showTemporary by lazy {
            !MyApplication.instance
                    .resources
                    .getBoolean(R.bool.release)
        }
    }

    protected val notificationManager by lazy { MyApplication.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private val lastNotificationBeeps = mutableMapOf<InstanceKey, Long>()

    private val notificationRelay = PublishRelay.create<() -> Unit>()

    init {
        notificationRelay.toFlowable(BackpressureStrategy.BUFFER)
                .observeOn(Schedulers.io())
                .flatMapSingle({ Single.fromCallable(it) }, false, 1)
                .subscribe()
    }

    override fun cancelNotification(id: Int, tag: String?) {
        Preferences.tickLog.logLineHour("NotificationManager.cancel id: $id, tag: $tag")
        notificationManager.cancel(tag, id)
    }

    private fun getHighPriority(): Boolean? = when (Preferences.notificationLevel) {
        Preferences.NotificationLevel.HIGH -> true
        Preferences.NotificationLevel.MEDIUM -> false
        Preferences.NotificationLevel.NONE -> null
    }

    final override fun notifyInstance(
            deviceDbInfo: DeviceDbInfo,
            instance: Instance<*>,
            silent: Boolean,
            now: ExactTimeStamp.Local,
    ) {
        val highPriority = getHighPriority() ?: return

        val instanceData = getInstanceData(deviceDbInfo, instance, silent, now, highPriority)
        notificationRelay.accept { notifyInstanceHelper(instanceData) }
    }

    protected open fun getInstanceData(
            deviceDbInfo: DeviceDbInfo,
            instance: Instance<*>,
            silent: Boolean,
            now: ExactTimeStamp.Local,
            highPriority: Boolean,
    ): InstanceData {
        val reallySilent = if (silent) {
            true
        } else {
            lastNotificationBeeps.values
                    .maxOrNull()
                    ?.takeIf { SystemClock.elapsedRealtime() - it < 5000 }
                    ?.let { true } ?: false
        }

        if (!reallySilent)
            lastNotificationBeeps[instance.instanceKey] = SystemClock.elapsedRealtime()

        return InstanceData(deviceDbInfo, instance, now, reallySilent, highPriority)
    }

    private fun notifyInstanceHelper(instanceData: InstanceData) {
        val notificationId = instanceData.notificationId

        val instanceKey = instanceData.instanceKey

        val pendingContentIntent = PendingIntent.getActivity(
                MyApplication.instance,
                notificationId,
                ShowInstanceActivity.getNotificationIntent(MyApplication.instance, instanceKey, notificationId),
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pendingDeleteIntent = NotificationActionReceiver.newPendingIntent(
                NotificationAction.DeleteInstanceNotification(instanceKey)
        )

        val pendingDoneIntent = NotificationActionReceiver.newPendingIntent(NotificationAction.InstanceDone(
                instanceKey,
                notificationId,
                instanceData.name
        ))

        val pendingHourIntent = NotificationActionReceiver.newPendingIntent(NotificationAction.InstanceHour(
                instanceKey,
                notificationId,
                instanceData.name
        ))

        fun action(
                @DrawableRes icon: Int,
                @StringRes text: Int,
                pendingIntent: PendingIntent
        ) = NotificationCompat.Action
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

        val sortKey = timeStampLong.toString() + doubleToString(instanceData.ordinal)

        val largeIcon = ImageManager.getLargeIcon(instanceData.uuid)

        val notificationHash = NotificationHash(
                instanceData.name,
                text,
                notificationId,
                timeStampLong,
                styleHash,
                sortKey,
                largeIcon?.let { instanceData.uuid!! },
                null
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
                notificationHash = notificationHash,
                tag = null,
                highPriority = instanceData.highPriority
        )
    }

    private fun doubleToString(d: Double): String {
        val bits = java.lang.Double.doubleToLongBits(d)
        val s = bits.toString()
        return (if (bits < 0) "--------------------" else "00000000000000000000").substring(s.length) + s
    }

    private fun getInstanceText(instance: Instance<*>, now: ExactTimeStamp.Local): String {
        val childNames = getChildNames(instance, now)

        return if (childNames.isNotEmpty()) {
            " (" + childNames.joinToString(", ") + ")"
        } else {
            val note = instance.task.note

            if (!note.isNullOrEmpty()) " ($note)" else ""
        }
    }

    private fun getChildNames(instance: Instance<*>, now: ExactTimeStamp.Local) = instance.getChildInstances(now)
            .asSequence()
            .filter { it.first.done == null }
            .sortedBy { it.second.childTask.ordinal }
            .map { it.first.name }
            .toList()

    protected open fun getExtraCount(lines: List<String>, group: Boolean) = lines.size - MAX_INBOX_LINES

    private fun getInboxStyle(
            lines: List<String>,
            group: Boolean
    ): Pair<() -> NotificationCompat.InboxStyle, NotificationHash.Style.Inbox> {
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
    protected open fun newBuilder(silent: Boolean, highPriority: Boolean) = NotificationCompat.Builder(MyApplication.instance)

    protected open fun getNotificationBuilder(
            title: String,
            text: String?,
            deleteIntent: PendingIntent?,
            contentIntent: PendingIntent,
            silent: Boolean,
            actions: List<NotificationCompat.Action>,
            time: Long?,
            style: (() -> NotificationCompat.Style)?,
            autoCancel: Boolean,
            summary: Boolean,
            sortKey: String,
            largeIcon: (() -> Bitmap)?,
            notificationHash: NotificationHash,
            highPriority: Boolean
    ): NotificationCompat.Builder {
        check(title.isNotEmpty())

        val priority = if (highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT

        val builder = newBuilder(silent, highPriority)
                .setContentTitle(title)
                .setSmallIcon(R.drawable.ikona_bez)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(priority)
                .setSortKey(sortKey)
                .setOnlyAlertOnce(true)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                .addExtras(Bundle().apply { putInt(KEY_HASH_CODE, notificationHash.hashCode()) })

        deleteIntent?.let { builder.setDeleteIntent(it) }

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

    protected open fun notify(
            title: String,
            text: String?,
            notificationId: Int,
            deleteIntent: PendingIntent?,
            contentIntent: PendingIntent,
            silent: Boolean,
            actions: List<NotificationCompat.Action>,
            time: Long?,
            style: (() -> NotificationCompat.Style)?,
            autoCancel: Boolean,
            summary: Boolean,
            sortKey: String,
            largeIcon: (() -> Bitmap)?,
            notificationHash: NotificationHash,
            tag: String?,
            highPriority: Boolean
    ) {
        val unchanged = notificationManager.activeNotifications
                ?.singleOrNull { it.id == notificationHash.id }
                ?.let { it.notification.extras.getInt(KEY_HASH_CODE) == notificationHash.hashCode() }
                ?: false

        if (unchanged) {
            Preferences.tickLog.logLineHour("skipping notification update for $title")
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
                notificationHash,
                highPriority
        ).build()

        @Suppress("Deprecation")
        if (!silent)
            notification.defaults = notification.defaults or Notification.DEFAULT_VIBRATE

        MyCrashlytics.log("NotificationManager.notify $notificationId silent? $silent")
        notificationManager.notify(tag, notificationId, notification)
    }

    override fun notifyGroup(
            instances: Collection<Instance<*>>,
            silent: Boolean, // not needed >= 24
            now: ExactTimeStamp.Local,
    ) {
        val highPriority = getHighPriority() ?: return

        notificationRelay.accept { notifyGroupHelper(GroupData(instances, now, silent, highPriority)) }
    }

    private inner class GroupData(
            instances: Collection<com.krystianwsul.common.firebase.models.Instance<*>>,
            private val now: ExactTimeStamp.Local,
            val silent: Boolean,
            val highPriority: Boolean,
    ) {

        val instances = instances.map(::Instance)

        inner class Instance(instance: com.krystianwsul.common.firebase.models.Instance<*>) {

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

        val pendingDeleteIntent = NotificationActionReceiver.newPendingIntent(
                NotificationAction.DeleteGroupNotification(instanceKeys)
        )

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
                NOTIFICATION_ID_GROUP,
                null,
                styleHash,
                "0",
                null,
                null
        )

        notify(
                title,
                text,
                NOTIFICATION_ID_GROUP,
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
                notificationHash = notificationHash,
                tag = null,
                highPriority = groupData.highPriority
        )
    }

    override fun cleanGroup(lastNotificationId: Int?) {
        check(Build.VERSION.SDK_INT < Build.VERSION_CODES.N)

        if (lastNotificationId != null)
            cancelNotification(lastNotificationId, null)
    }

    override fun updateAlarm(nextAlarm: TimeStamp?) {
        val pendingIntent = PendingIntent.getBroadcast(MyApplication.instance, 0, AlarmReceiver.newIntent(), PendingIntent.FLAG_UPDATE_CURRENT)!!
        alarmManager.cancel(pendingIntent)
        nextAlarm?.let { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it.long, pendingIntent) }
    }

    override fun logNotificationIds(source: String) = Unit

    override fun notifyTemporary(notificationId: Int, source: String) {
        Preferences.temporaryNotificationLog.logLineDate("notifyTemporary $source")

        if (!showTemporary) return

        val highPriority = getHighPriority() ?: return

        val pendingContentIntent = PendingIntent.getActivity(
                MyApplication.context,
                0,
                MainActivity.newIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        notify(
                source,
                null,
                notificationId,
                null,
                pendingContentIntent,
                true,
                listOf(),
                null,
                null,
                autoCancel = true,
                summary = false,
                sortKey = "1",
                largeIcon = null,
                notificationHash = NotificationHash(
                        source,
                        null,
                        notificationId,
                        null,
                        null,
                        "1",
                        null,
                        TAG_TEMPORARY
                ),
                TAG_TEMPORARY,
                highPriority
        )
    }

    override fun hideTemporary(notificationId: Int, source: String) {
        Preferences.temporaryNotificationLog.logLineHour("hideTemporary $source")

        cancelNotification(notificationId, TAG_TEMPORARY)
    }

    protected data class NotificationHash(
            val title: String,
            val text: String?,
            val id: Int,
            val timeStamp: Long?,
            val style: Style?,
            val sortKey: String,
            val uuid: String?,
            val tag: String?
    ) {

        interface Style {

            data class Inbox(val lines: List<String>, val extraCount: Int) : Style
            data class Text(val text: String?) : Style
            data class Picture(val uuid: String) : Style
        }
    }

    protected inner class InstanceData(
            deviceDbInfo: DeviceDbInfo,
            instance: Instance<*>,
            now: ExactTimeStamp.Local,
            val silent: Boolean,
            val highPriority: Boolean,
    ) {

        val notificationId = instance.notificationId

        val instanceKey = instance.instanceKey

        val note = instance.task.note

        val uuid = instance.task
                .getImage(deviceDbInfo)
                ?.uuid

        val timeStampLong = instance.instanceDateTime
                .timeStamp
                .long

        val ordinal = instance.task.ordinal

        val name = instance.name

        val childNames = getChildNames(instance, now)
    }
}