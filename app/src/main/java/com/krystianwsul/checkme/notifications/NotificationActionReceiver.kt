package com.krystianwsul.checkme.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.krystianwsul.checkme.MyApplication

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
                PendingIntent.FLAG_UPDATE_CURRENT
        )!!
    }

    override fun onReceive(context: Context, intent: Intent) {
        // todo notify temporary

        val notificationAction = intent.getParcelableExtra<NotificationAction>(KEY_NOTIFICATION_ACTION)!!

        notificationAction.perform() // todo hide temporary in callback
    }
}