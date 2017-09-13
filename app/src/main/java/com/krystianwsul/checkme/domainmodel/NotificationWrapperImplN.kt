package com.krystianwsul.checkme.domainmodel

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import junit.framework.Assert

@SuppressLint("NewApi")
class NotificationWrapperImplN : NotificationWrapperImplM() {

    override fun cleanGroup(context: Context, lastNotificationId: Int?) {
        Assert.assertTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val statusBarNotifications = notificationManager.activeNotifications!!

        if (lastNotificationId != null) {
            if (statusBarNotifications.size > 2) {
                cancelNotification(context, lastNotificationId)
            } else if (statusBarNotifications.size < 2) {
                Assert.assertTrue(statusBarNotifications.isEmpty())
            } else {
                Assert.assertTrue(statusBarNotifications.size == 2)

                if (statusBarNotifications.none { it.id == 0 })
                    NotificationException.throwException(lastNotificationId, statusBarNotifications)

                if (statusBarNotifications.any { it.id == lastNotificationId }) {
                    cancelNotification(context, 0)
                    cancelNotification(context, lastNotificationId)
                }
            }
        } else {
            if (statusBarNotifications.size != 1)
                return

            Log.e("asdf", "cleaning group")

            Assert.assertTrue(statusBarNotifications.single().id == 0)

            cancelNotification(context, 0)
        }
    }

    private class NotificationException(message: String) : RuntimeException(message) {

        companion object {

            fun throwException(lastNotificationId: Int, statusBarNotifications: Array<StatusBarNotification>) {
                throw NotificationException("last id: $lastNotificationId, shown ids: " + statusBarNotifications.joinToString(", ") { it.id.toString() })
            }
        }
    }
}