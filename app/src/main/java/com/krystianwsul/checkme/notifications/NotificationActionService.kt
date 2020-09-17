package com.krystianwsul.checkme.notifications

import android.app.IntentService
import android.content.Context
import android.content.Intent

class NotificationActionService : IntentService("NotificationActionService") {

    companion object {

        private const val KEY_NOTIFICATION_ACTION = "notificationAction"

        fun newIntent(context: Context, notificationAction: NotificationAction) = Intent(context, NotificationActionService::class.java).apply {
            putExtra(KEY_NOTIFICATION_ACTION, notificationAction)
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        val notificationAction = intent!!.getParcelableExtra<NotificationAction>(KEY_NOTIFICATION_ACTION)!!

        notificationAction.perform()
    }
}