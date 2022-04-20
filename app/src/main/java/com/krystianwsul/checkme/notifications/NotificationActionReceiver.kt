package com.krystianwsul.checkme.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.firebase.database.TaskPriorityMapperQueue

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {

        private const val KEY_NOTIFICATION_ACTION = "notificationAction"

        fun newIntent(notificationAction: NotificationAction) = Intent(
                MyApplication.context,
                NotificationActionReceiver::class.java
        ).apply { putExtra(KEY_NOTIFICATION_ACTION, notificationAction) }

        fun newPendingIntent(notificationAction: NotificationAction) = PendingIntent.getBroadcast(
            MyApplication.context,
            notificationAction.requestCode,
            newIntent(notificationAction),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )!!
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationAction = intent.getParcelableExtra<NotificationAction>(KEY_NOTIFICATION_ACTION)!!

        TaskPriorityMapperQueue.addProvider(notificationAction)

        val source = "NotificationActionReceiver $notificationAction"

        NotificationWrapper.instance.notifyTemporary(notificationAction.requestCode, source)

        notificationAction.perform().subscribe {
            TaskPriorityMapperQueue.removeProvider(notificationAction)

            NotificationWrapper.instance.hideTemporary(notificationAction.requestCode, source)
        }
    }
}