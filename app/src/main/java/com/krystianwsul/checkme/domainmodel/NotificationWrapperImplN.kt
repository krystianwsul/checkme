package com.krystianwsul.checkme.domainmodel

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.notifications.TickJobIntentService


@SuppressLint("NewApi")
open class NotificationWrapperImplN : NotificationWrapperImplM() {

    override fun cleanGroup(lastNotificationId: Int?) {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)

        val statusBarNotifications = notificationManager.activeNotifications!!

        if (lastNotificationId != null) {
            if (statusBarNotifications.size > 2) {
                cancelNotification(lastNotificationId)
            } else if (statusBarNotifications.size < 2) {
                if (statusBarNotifications.isNotEmpty())
                    MyCrashlytics.logException(NotificationException(lastNotificationId, statusBarNotifications))

                // guessing, basically
                cancelNotification(0)
                cancelNotification(lastNotificationId)
            } else {
                check(statusBarNotifications.size == 2)

                if (statusBarNotifications.none { it.id == 0 })
                    throw NotificationException(lastNotificationId, statusBarNotifications)

                if (statusBarNotifications.any { it.id == lastNotificationId }) {
                    cancelNotification(0)
                    cancelNotification(lastNotificationId)
                }
            }
        } else {
            if (statusBarNotifications.size != 1)
                return

            check(statusBarNotifications.single().id == 0)

            cancelNotification(0)
        }
    }

    override fun getInboxStyle(lines: List<String>, group: Boolean): NotificationCompat.InboxStyle {
        check(lines.isNotEmpty())

        val max = 5

        val inboxStyle = NotificationCompat.InboxStyle()

        lines.take(max).forEach { inboxStyle.addLine(it) }

        val extraCount = lines.size - max

        if (extraCount > 0 && !group)
            inboxStyle.setSummaryText("+" + extraCount + " " + MyApplication.instance.getString(R.string.more))

        return inboxStyle
    }

    override fun getNotificationBuilder(title: String, text: String?, deleteIntent: PendingIntent, contentIntent: PendingIntent, silent: Boolean, actions: List<NotificationCompat.Action>, time: Long?, style: NotificationCompat.Style?, autoCancel: Boolean, summary: Boolean, sortKey: String): NotificationCompat.Builder {
        return super.getNotificationBuilder(title, text, deleteIntent, contentIntent, silent, actions, time, style, autoCancel, summary, sortKey).apply {
            setGroup(TickJobIntentService.GROUP_KEY)

            if (summary)
                setGroupSummary(true)
        }
    }

    private class NotificationException(lastNotificationId: Int, statusBarNotifications: Array<StatusBarNotification>) : RuntimeException("last id: $lastNotificationId, shown ids: " + statusBarNotifications.joinToString(", ") { it.id.toString() })
}