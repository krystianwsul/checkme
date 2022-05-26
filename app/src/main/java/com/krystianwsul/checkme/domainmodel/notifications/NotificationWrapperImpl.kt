package com.krystianwsul.checkme.domainmodel.notifications

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.instances.ShowGroupActivity
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.gui.instances.ShowNotificationGroupActivity
import com.krystianwsul.checkme.gui.main.MainActivity
import com.krystianwsul.checkme.notifications.NotificationAction
import com.krystianwsul.checkme.notifications.NotificationActionReceiver
import com.krystianwsul.checkme.ticks.AlarmReceiver
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.project.SharedOwnedProject
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.Ordinal
import com.krystianwsul.common.utils.ProjectKey
import com.mindorks.scheduler.Priority
import com.mindorks.scheduler.internal.CustomPriorityScheduler
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Single

open class NotificationWrapperImpl : NotificationWrapper() {

    companion object {

        val alarmManager by lazy { MyApplication.instance.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

        @JvmStatic
        protected val KEY_HASH_CODE = "com.krystianwsul.checkme.notification_hash_code"

        const val NOTIFICATION_ID_GROUP = 0
        const val NOTIFICATION_ID_GROUP_NOT_SUMMARY = 1

        private const val TAG_TEMPORARY = "temporary"

        private const val GROUP_KEY = "group"

        private const val HIGH_CHANNEL_ID = "channel"

        private val HIGH_CHANNEL = NotificationChannel(
            HIGH_CHANNEL_ID,
            "Heads-up reminders",
            NotificationManager.IMPORTANCE_HIGH,
        )

        private const val MEDIUM_CHANNEL_ID = "mediumChannel"

        private val MEDIUM_CHANNEL = NotificationChannel(
            MEDIUM_CHANNEL_ID,
            "Regular reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        )

        val showTemporary by lazy {
            !MyApplication.instance
                .resources
                .getBoolean(R.bool.release)
        }

        fun getProjectNotificationId(projectKey: ProjectKey.Shared, timeStamp: TimeStamp) =
            (projectKey.hashCode() + timeStamp.long).toInt()

        private fun getInstanceText(instance: Instance, now: ExactTimeStamp.Local): String {
            val childNames = getInstanceNames(instance.getChildInstances(), now, true)

            return getInstanceText(childNames, instance.task.note)
        }

        private fun getInstanceText(childNames: List<String>, note: String?): String {
            return if (childNames.isNotEmpty()) {
                " (" + childNames.joinToString(", ") + ")"
            } else if (!note.isNullOrEmpty()) {
                " ($note)"
            } else {
                ""
            }
        }

        private fun getInstanceNames(
            instances: List<Instance>,
            now: ExactTimeStamp.Local,
            assumeChild: Boolean,
        ) = instances.asSequence()
            .filter { it.done == null }
            .filter { it.isVisible(now, Instance.VisibilityOptions(assumeChildOfVisibleParent = assumeChild)) }
            .sortedBy { it.ordinal }
            .map { it.name }
            .toList()
    }

    protected open val maxInboxLines = 5

    private val notificationManager by lazy {
        MyApplication.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val notificationRelay = PublishRelay.create<() -> Unit>()

    init {
        HIGH_CHANNEL.enableVibration(true)
        MEDIUM_CHANNEL.enableVibration(true)

        notificationManager.createNotificationChannels(listOf(HIGH_CHANNEL, MEDIUM_CHANNEL))

        val scheduler = CustomPriorityScheduler.create().get(Priority.MEDIUM)

        notificationRelay.toFlowable(BackpressureStrategy.BUFFER)
            .observeOn(scheduler)
            .flatMapSingle({ Single.fromCallable(it) }, false, 1)
            .subscribe()
    }

    override fun cancelNotification(id: Int, tag: String?) {
        notificationRelay.accept {
            Preferences.tickLog.logLineHour("NotificationManager.cancel id: $id, tag: $tag")
            Preferences.notificationLog.logLineHour("cancel id: $id, tag: $tag", true)

            notificationManager.cancel(tag, id)
        }
    }

    private fun getHighPriority(): Boolean? = when (Preferences.notificationLevel) {
        Preferences.NotificationLevel.HIGH -> true
        Preferences.NotificationLevel.MEDIUM -> false
        Preferences.NotificationLevel.NONE -> null
    }

    final override fun notifyInstance(
        deviceDbInfo: DeviceDbInfo,
        instance: Instance,
        silent: Boolean,
        now: ExactTimeStamp.Local,
        ordinal: Ordinal,
    ) {
        val highPriority = getHighPriority() ?: return

        val instanceData = getInstanceData(deviceDbInfo, instance, silent, now, highPriority, ordinal)
        notificationRelay.accept { notifyInstanceHelper(instanceData) }
    }

    override fun notifyProject(
        project: SharedOwnedProject,
        instances: List<Instance>,
        timeStamp: TimeStamp,
        silent: Boolean,
        now: ExactTimeStamp.Local,
        ordinal: Ordinal,
    ) {
        val highPriority = getHighPriority() ?: return

        val projectData = ProjectData(project, instances, now, silent, highPriority, timeStamp, ordinal)
        notificationRelay.accept { notifyProjectHelper(projectData) }
    }

    private fun getInstanceData(
        deviceDbInfo: DeviceDbInfo,
        instance: Instance,
        silent: Boolean,
        now: ExactTimeStamp.Local,
        highPriority: Boolean,
        ordinal: Ordinal,
    ) = InstanceData(deviceDbInfo, instance, now, silent, highPriority, ordinal)

    private fun notifyInstanceHelper(instanceData: InstanceData) {
        val notificationId = instanceData.notificationId

        val instanceKey = instanceData.instanceKey

        val pendingContentIntent = PendingIntent.getActivity(
            MyApplication.instance,
            notificationId,
            ShowInstanceActivity.getNotificationIntent(MyApplication.instance, instanceKey),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val pendingDeleteIntent = NotificationActionReceiver.newPendingIntent(
            NotificationAction.Instance.Delete(instanceKey)
        )

        val pendingDoneIntent = NotificationActionReceiver.newPendingIntent(
            NotificationAction.Instance.Done(instanceKey, notificationId)
        )

        val pendingHourIntent = NotificationActionReceiver.newPendingIntent(
            NotificationAction.Instance.Hour(instanceKey, notificationId)
        )

        fun action(
            @DrawableRes icon: Int,
            @StringRes text: Int,
            pendingIntent: PendingIntent,
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

            val pair = getInboxStyle(childNames, false, instanceData.project)
            style = pair.first
            styleHash = pair.second
        } else if (!instanceData.note.isNullOrEmpty()) {
            text = listOfNotNull(instanceData.project, instanceData.note).joinToString("\n")

            style = {
                NotificationCompat.BigTextStyle().also { it.bigText(text) }
            }

            styleHash = NotificationHash.Style.Text(text)
        } else {
            text = instanceData.project

            val bigPicture = ImageManager.getBigPicture(instanceData.uuid)
            if (bigPicture != null) {
                style = {
                    NotificationCompat.BigPictureStyle().also {
                        it.bigPicture(bigPicture())
                        it.bigLargeIcon(null)
                    }
                }
                styleHash = NotificationHash.Style.Picture(instanceData.uuid!!)
            } else {
                style = null
                styleHash = null
            }
        }

        val timeStampLong = instanceData.timeStampLong

        val sortKey = timeStampLong.toString() + instanceData.ordinal.padded()

        val largeIcon = ImageManager.getLargeIcon(instanceData.uuid)

        val notificationHash = NotificationHash(
            instanceData.name,
            text,
            notificationId,
            timeStampLong,
            styleHash,
            sortKey,
            largeIcon?.let { instanceData.uuid!! },
            null,
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
            summary = false,
            sortKey = sortKey,
            largeIcon = largeIcon,
            notificationHash = notificationHash,
            tag = null,
            highPriority = instanceData.highPriority,
        )
    }

    private fun notifyProjectHelper(projectData: ProjectData) {
        val notificationId = projectData.notificationId

        val pendingContentIntent = PendingIntent.getActivity(
            MyApplication.instance,
            notificationId,
            ShowGroupActivity.getIntent(
                MyApplication.instance,
                ShowGroupActivity.Parameters.Project(projectData.timeStamp, projectData.projectKey),
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val pendingDeleteIntent = NotificationActionReceiver.newPendingIntent(
            NotificationAction.Project.Delete(projectData.projectKey, projectData.timeStamp)
        )

        val pendingDoneIntent = NotificationActionReceiver.newPendingIntent(
            NotificationAction.Project.Done(projectData.projectKey, projectData.timeStamp, notificationId)
        )

        val pendingHourIntent = NotificationActionReceiver.newPendingIntent(
            NotificationAction.Project.Hour(projectData.projectKey, projectData.timeStamp, notificationId)
        )

        fun action(
            @DrawableRes icon: Int,
            @StringRes text: Int,
            pendingIntent: PendingIntent,
        ) = NotificationCompat.Action
            .Builder(icon, MyApplication.instance.getString(text), pendingIntent)
            .build()

        val actions = listOf(
            action(R.drawable.ic_done_white_24dp, R.string.done, pendingDoneIntent),
            action(R.drawable.ic_alarm_white_24dp, R.string.hour, pendingHourIntent),
        )

        val childNames = projectData.childNames

        val text = childNames.joinToString(", ")
        val (style, styleHash) = getInboxStyle(childNames, false, null)

        val timeStampLong = projectData.timeStamp.long

        val sortKey = timeStampLong.toString() + projectData.ordinal.padded()

        val notificationHash = NotificationHash(
            projectData.name,
            text,
            notificationId,
            timeStampLong,
            styleHash,
            sortKey,
            null,
            null,
        )

        notify(
            projectData.name,
            text,
            notificationId,
            pendingDeleteIntent,
            pendingContentIntent,
            projectData.silent,
            actions,
            timeStampLong,
            style,
            summary = false,
            sortKey = sortKey,
            largeIcon = null,
            notificationHash = notificationHash,
            tag = null,
            highPriority = projectData.highPriority
        )
    }

    protected open fun getExtraCount(lines: List<String>, summary: Boolean) = if (summary) 0 else lines.size - maxInboxLines

    private fun getInboxStyle(
        lines: List<String>,
        summary: Boolean,
        extraDescription: String? = null,
    ): Pair<() -> NotificationCompat.InboxStyle, NotificationHash.Style.Inbox> {
        check(lines.isNotEmpty())

        val inboxStyle = NotificationCompat.InboxStyle()

        inboxStyle.setBigContentTitle(null)

        val finalLines = lines.take(maxInboxLines)

        finalLines.forEach { inboxStyle.addLine(it) }

        val extraCount = getExtraCount(lines, summary)

        if (extraCount > 0)
            inboxStyle.setSummaryText("+" + extraCount + " " + MyApplication.instance.getString(R.string.more))
        else if (extraDescription != null)
            inboxStyle.setSummaryText(extraDescription)

        return Pair({ inboxStyle }, NotificationHash.Style.Inbox(finalLines, extraCount))
    }

    protected open fun newBuilder(highPriority: Boolean) = NotificationCompat.Builder(
        MyApplication.instance,
        when {
            highPriority -> HIGH_CHANNEL_ID
            else -> MEDIUM_CHANNEL_ID
        },
    )

    protected open fun getNotificationBuilder(
        title: String?,
        text: String?,
        deleteIntent: PendingIntent?,
        contentIntent: PendingIntent,
        silent: Boolean,
        actions: List<NotificationCompat.Action>,
        time: Long?,
        style: (() -> NotificationCompat.Style)?,
        summary: Boolean,
        sortKey: String,
        largeIcon: (() -> Bitmap)?,
        notificationHash: NotificationHash,
        highPriority: Boolean,
    ): NotificationCompat.Builder {
        val priority = if (highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT

        val builder = newBuilder(highPriority)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ikona_bez)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(priority)
            .setSortKey(sortKey)
            .setSilent(silent)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .addExtras(Bundle().apply { putInt(KEY_HASH_CODE, notificationHash.hashCode()) })

        deleteIntent?.let { builder.setDeleteIntent(it) }

        if (!text.isNullOrEmpty()) builder.setContentText(text)

        check(actions.size <= 3)

        actions.forEach { builder.addAction(it) }

        time?.let { builder.setWhen(it).setShowWhen(true) }

        style?.let { builder.setStyle(it()) }

        largeIcon?.let { builder.setLargeIcon(it()) }

        builder.setGroup(GROUP_KEY)

        if (summary) builder.setGroupSummary(true)

        return builder
    }

    protected fun notify(
        title: String?,
        text: String?,
        notificationId: Int,
        deleteIntent: PendingIntent?,
        contentIntent: PendingIntent,
        silent: Boolean,
        actions: List<NotificationCompat.Action>,
        time: Long?,
        style: (() -> NotificationCompat.Style)?,
        summary: Boolean,
        sortKey: String,
        largeIcon: (() -> Bitmap)?,
        notificationHash: NotificationHash,
        tag: String?,
        highPriority: Boolean,
    ) {
        val notificationHashCode = notificationHash.hashCode()

        val unchanged = notificationManager.activeNotifications
            ?.singleOrNull { it.id == notificationHash.id }
            ?.let { it.notification.extras.getInt(KEY_HASH_CODE) == notificationHashCode }
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
            summary,
            sortKey,
            largeIcon,
            notificationHash,
            highPriority,
        ).build()

        // I don't think this has any effect, with channels
        @Suppress("Deprecation")
        if (!silent)
            notification.defaults = notification.defaults or Notification.DEFAULT_VIBRATE

        notification.extras.putInt(KEY_HASH_CODE, notificationHashCode)

        MyCrashlytics.log("NotificationManager.notify $notificationId silent? $silent")

        val previousSortKey = notificationManager.activeNotifications
            .singleOrNull { it.id == notificationId && it.tag == tag }
            ?.notification
            ?.sortKey

        val sortKeyMessage = when (previousSortKey) {
            null -> "null"
            sortKey -> "same"
            else -> "changed"
        }

        Preferences.notificationLog.logLineHour(
            "notify:" +
                    "\nid: $notificationId" +
                    "\ntag: $tag" +
                    "\nsummary: $summary" +
                    "\nsortKey: $sortKeyMessage" +
                    "\ntitle: $title",
            true,
        )

        notificationManager.notify(tag, notificationId, notification)
    }

    override fun notifyGroup(
        instances: Collection<Instance>,
        silent: Boolean,
        now: ExactTimeStamp.Local,
        summary: Boolean,
        projects: Collection<GroupTypeFactory.Notification.Project>,
    ) {
        val highPriority = getHighPriority() ?: return

        val groupData = GroupData(instances, projects, now, silent, highPriority, summary)
        notificationRelay.accept { notifyGroupHelper(groupData) }
    }

    private class GroupData(
        instances: Collection<Instance>,
        projects: Collection<GroupTypeFactory.Notification.Project>,
        val now: ExactTimeStamp.Local,
        val silent: Boolean,
        val highPriority: Boolean,
        val summary: Boolean,
    ) {
        val items = (instances.map { Item.Instance(it, now) } + projects.map(Item::Project)).sorted()

        sealed interface Item : Comparable<Item> {

            val name: String
            val text: String
            val timeStamp: TimeStamp

            class Instance(instance: com.krystianwsul.common.firebase.models.Instance, now: ExactTimeStamp.Local) : Item {

                override val name = instance.name
                override val text = getInstanceText(instance, now)
                override val timeStamp = instance.instanceDateTime.timeStamp

                private val ordinal = instance.ordinal

                override fun compareTo(other: Item): Int {
                    timeStamp.compareTo(other.timeStamp)
                        .takeIf { it != 0 }
                        ?.let { return it }

                    return when (other) {
                        is Instance -> ordinal.compareTo(other.ordinal)
                        is Project -> 1
                    }
                }
            }

            class Project(project: GroupTypeFactory.Notification.Project) : Item {

                override val name = project.project.name
                override val text = getInstanceText(project.instances.map { it.name }, null)
                override val timeStamp = project.timeStamp

                private val ordinal = project.instances.minOf { it.ordinal }

                override fun compareTo(other: Item): Int {
                    timeStamp.compareTo(other.timeStamp)
                        .takeIf { it != 0 }
                        ?.let { return it }

                    return when (other) {
                        is Instance -> -1
                        is Project -> return ordinal.compareTo(other.ordinal)
                    }
                }
            }
        }
    }

    private fun notifyGroupHelper(groupData: GroupData) {
        val pendingDeleteIntent = NotificationActionReceiver.newPendingIntent(NotificationAction.DeleteGroupNotification)

        val notificationId = if (groupData.summary) NOTIFICATION_ID_GROUP else NOTIFICATION_ID_GROUP_NOT_SUMMARY

        val pendingContentIntent = PendingIntent.getActivity(
            MyApplication.instance,
            notificationId,
            ShowNotificationGroupActivity.getIntent(MyApplication.instance),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val (inboxStyle, styleHash) = getInboxStyle(groupData.items.map { it.name + it.text }, groupData.summary)

        val title =
            if (groupData.summary) groupData.items.size.toString() + " " + MyApplication.instance.getString(R.string.multiple_reminders) else null
        val text = groupData.items.joinToString(", ") { it.name }

        val notificationHash = NotificationHash(
            title,
            text,
            notificationId,
            null,
            styleHash,
            notificationId.toString(),
            null,
            null,
        )

        notify(
            title,
            text,
            notificationId,
            pendingDeleteIntent,
            pendingContentIntent,
            groupData.silent,
            listOf(),
            null,
            inboxStyle,
            summary = groupData.summary,
            sortKey = notificationId.toString(),
            largeIcon = null,
            notificationHash = notificationHash,
            tag = null,
            highPriority = groupData.highPriority,
        )
    }

    override fun cleanGroup(lastNotificationId: Int?) {
        notificationRelay.accept {
            val statusBarNotifications = notificationManager.activeNotifications!!.filter { it.tag == null }

            if (lastNotificationId != null) {
                when (statusBarNotifications.size) {
                    in (0..1) -> {
                        if (statusBarNotifications.isNotEmpty())
                            MyCrashlytics.logException(NotificationException(lastNotificationId, statusBarNotifications))

                        // guessing, basically
                        cancelNotification(NOTIFICATION_ID_GROUP)
                        cancelNotification(lastNotificationId)
                    }
                    2 -> {
                        if (statusBarNotifications.none { it.id == NOTIFICATION_ID_GROUP })
                            throw NotificationException(lastNotificationId, statusBarNotifications)

                        if (statusBarNotifications.any { it.id == lastNotificationId }) {
                            cancelNotification(NOTIFICATION_ID_GROUP)
                            cancelNotification(lastNotificationId)
                        }
                    }
                    else -> cancelNotification(lastNotificationId)
                }
            } else {
                if (statusBarNotifications.size != 1) return@accept

                check(statusBarNotifications.single().id == NOTIFICATION_ID_GROUP)

                cancelNotification(NOTIFICATION_ID_GROUP)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun updateAlarm(nextAlarm: TimeStamp?) {
        notificationRelay.accept {
            val pendingIntent = PendingIntent.getBroadcast(
                MyApplication.instance,
                0,
                AlarmReceiver.newIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )!!

            alarmManager.cancel(pendingIntent)
            nextAlarm?.let { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it.long, pendingIntent) }
        }
    }

    override fun notifyTemporary(notificationId: Int, source: String) {
        notificationRelay.accept {
            Preferences.temporaryNotificationLog.logLineDate("notifyTemporary $source")

            if (!showTemporary) return@accept

            val highPriority = getHighPriority() ?: return@accept

            val pendingContentIntent = PendingIntent.getActivity(
                MyApplication.context,
                notificationId,
                MainActivity.newIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
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
                summary = false,
                sortKey = notificationId.toString(),
                largeIcon = null,
                notificationHash = NotificationHash(
                    source,
                    null,
                    notificationId,
                    null,
                    null,
                    notificationId.toString(),
                    null,
                    TAG_TEMPORARY
                ),
                TAG_TEMPORARY,
                highPriority,
            )
        }
    }

    override fun hideTemporary(notificationId: Int, source: String) {
        notificationRelay.accept {
            Preferences.temporaryNotificationLog.logLineHour("hideTemporary $source")

            cancelNotification(notificationId, TAG_TEMPORARY)
        }
    }

    protected data class NotificationHash(
        val title: String?,
        val text: String?,
        val id: Int,
        val timeStamp: Long?,
        val style: Style?,
        val sortKey: String,
        val uuid: String?,
        val tag: String?,
    ) {

        interface Style {

            data class Inbox(val lines: List<String>, val extraCount: Int) : Style
            data class Text(val text: String?) : Style
            data class Picture(val uuid: String) : Style
        }
    }

    protected inner class InstanceData(
        deviceDbInfo: DeviceDbInfo,
        instance: Instance,
        now: ExactTimeStamp.Local,
        val silent: Boolean,
        val highPriority: Boolean,
        val ordinal: Ordinal,
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

        val name = instance.name

        val childNames = getInstanceNames(instance.getChildInstances(), now, true)

        val project = instance.getProject()
            .let { it as? SharedOwnedProject }
            ?.name
    }

    protected inner class ProjectData(
        project: SharedOwnedProject,
        instances: List<Instance>,
        now: ExactTimeStamp.Local,
        val silent: Boolean,
        val highPriority: Boolean,
        val timeStamp: TimeStamp,
        val ordinal: Ordinal,
    ) {

        val projectKey = project.projectKey

        val notificationId = getProjectNotificationId(projectKey, timeStamp)

        val name = project.name

        val childNames = getInstanceNames(instances, now, false)
    }

    class NotificationException(
        lastNotificationId: Int,
        statusBarNotifications: Iterable<StatusBarNotification>,
    ) : RuntimeException(
        "last id: $lastNotificationId, shown ids: " +
                statusBarNotifications.joinToString(", ") { it.id.toString() }
    )
}