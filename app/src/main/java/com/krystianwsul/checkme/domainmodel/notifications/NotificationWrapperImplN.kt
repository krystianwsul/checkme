package com.krystianwsul.checkme.domainmodel.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.graphics.Bitmap
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences


@SuppressLint("NewApi")
open class NotificationWrapperImplN : NotificationWrapperImpl() {

    companion object {

        private const val GROUP_KEY = "group"
    }

    override fun cleanGroup(lastNotificationId: Int?) {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)

        val statusBarNotifications = notificationManager.activeNotifications!!.filter { it.tag == null }

        if (lastNotificationId != null) {
            when (statusBarNotifications.size) {
                in (0..1) -> {
                    if (statusBarNotifications.isNotEmpty())
                        MyCrashlytics.logException(NotificationException(lastNotificationId, statusBarNotifications))

                    // guessing, basically
                    cancelNotification(0)
                    cancelNotification(lastNotificationId)
                }
                2 -> {
                    if (statusBarNotifications.none { it.id == 0 })
                        throw NotificationException(lastNotificationId, statusBarNotifications)

                    if (statusBarNotifications.any { it.id == lastNotificationId }) {
                        cancelNotification(0)
                        cancelNotification(lastNotificationId)
                    }
                }
                else -> cancelNotification(lastNotificationId)
            }
        } else {
            if (statusBarNotifications.size != 1)
                return

            check(statusBarNotifications.single().id == 0)

            cancelNotification(0)
        }
    }

    override fun getExtraCount(
            lines: List<String>,
            summary: Boolean,
    ) = if (summary) 0 else lines.size - maxInboxLines

    override fun getNotificationBuilder(
            title: String?,
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
            highPriority: Boolean,
    ) = super.getNotificationBuilder(
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
    ).apply {
        setGroup(GROUP_KEY)

        if (summary) setGroupSummary(true)
    }

    override fun logNotificationIds(source: String) = Preferences.tickLog.logLineHour("NotificationManager ids ($source): " + notificationManager.activeNotifications.map { it.id })

    private class NotificationException(
            lastNotificationId: Int,
            statusBarNotifications: Iterable<StatusBarNotification>
    ) : RuntimeException("last id: $lastNotificationId, shown ids: " + statusBarNotifications.joinToString(", ") { it.id.toString() })
}