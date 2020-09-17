package com.krystianwsul.checkme.notifications

import android.app.IntentService
import android.app.PendingIntent
import android.content.Intent
import com.krystianwsul.checkme.MyApplication

class NotificationActionService : IntentService("NotificationActionService") {

    companion object {

        private const val KEY_NOTIFICATION_ACTION = "notificationAction"

        private fun newIntent(notificationAction: NotificationAction) = Intent(
                MyApplication.context,
                NotificationActionService::class.java
        ).apply { putExtra(KEY_NOTIFICATION_ACTION, notificationAction) }

        fun newPendingIntent(notificationAction: NotificationAction) = PendingIntent.getService(
                MyApplication.context,
                notificationAction.requestCode,
                newIntent(notificationAction),
                PendingIntent.FLAG_UPDATE_CURRENT
        )!!
    }

    override fun onHandleIntent(intent: Intent?) {
        val notificationAction = intent!!.getParcelableExtra<NotificationAction>(KEY_NOTIFICATION_ACTION)!!

        notificationAction.perform()
    }
}