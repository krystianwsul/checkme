package com.krystianwsul.checkme.domainmodel

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.os.Build
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.notifications.TickService
import junit.framework.Assert

@SuppressLint("NewApi")
open class NotificationWrapperImplN : NotificationWrapperImpl() {

    override fun cleanGroup(lastNotificationId: Int?) {
        Assert.assertTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)

        val statusBarNotifications = notificationManager.activeNotifications!!

        if (lastNotificationId != null) {
            if (statusBarNotifications.size > 2) {
                cancelNotification(lastNotificationId)
            } else if (statusBarNotifications.size < 2) {
                Assert.assertTrue(statusBarNotifications.isEmpty())
            } else {
                Assert.assertTrue(statusBarNotifications.size == 2)

                if (statusBarNotifications.none { it.id == 0 })
                    NotificationException.throwException(lastNotificationId, statusBarNotifications)

                if (statusBarNotifications.any { it.id == lastNotificationId }) {
                    cancelNotification(0)
                    cancelNotification(lastNotificationId)
                }
            }
        } else {
            if (statusBarNotifications.size != 1)
                return

            Log.e("asdf", "cleaning group")

            Assert.assertTrue(statusBarNotifications.single().id == 0)

            cancelNotification(0)
        }
    }

    override fun getInboxStyle(lines: List<String>, group: Boolean): NotificationCompat.InboxStyle {
        Assert.assertTrue(!lines.isEmpty())

        val max = 5

        val inboxStyle = NotificationCompat.InboxStyle()

        lines.take(max).forEach { inboxStyle.addLine(it) }

        val extraCount = lines.size - max

        if (extraCount > 0 && !group)
            inboxStyle.setSummaryText("+" + extraCount + " " + MyApplication.instance.getString(R.string.more))

        return inboxStyle
    }

    override fun notify(title: String, text: String?, notificationId: Int, deleteIntent: PendingIntent, contentIntent: PendingIntent, silent: Boolean, actions: List<NotificationCompat.Action>, time: Long?, style: NotificationCompat.Style?, autoCancel: Boolean, summary: Boolean, sortKey: String) {
        Assert.assertTrue(title.isNotEmpty())

        @Suppress("Deprecation")
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

        Assert.assertTrue(actions.size <= 3)

        actions.forEach { builder.addAction(it) }

        if (time != null)
            builder.setWhen(time).setShowWhen(true)

        if (style != null)
            builder.setStyle(style)

        if (autoCancel)
            builder.setAutoCancel(true)

        builder.setGroup(TickService.GROUP_KEY)

        if (summary)
            builder.setGroupSummary(true)

        val notification = builder.build()

        @Suppress("Deprecation")
        if (!silent)
            notification.defaults = notification.defaults or Notification.DEFAULT_VIBRATE

        MyCrashlytics.log("NotificationManager.notify " + notificationId)
        notificationManager.notify(notificationId, notification)
    }

    private class NotificationException(message: String) : RuntimeException(message) {

        companion object {

            fun throwException(lastNotificationId: Int, statusBarNotifications: Array<StatusBarNotification>) {
                throw NotificationException("last id: $lastNotificationId, shown ids: " + statusBarNotifications.joinToString(", ") { it.id.toString() })
            }
        }
    }
}