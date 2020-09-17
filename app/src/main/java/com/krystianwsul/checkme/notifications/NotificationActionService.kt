package com.krystianwsul.checkme.notifications

import android.app.IntentService
import android.app.PendingIntent
import android.content.Intent
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapperImpl

class NotificationActionService : IntentService("NotificationActionService") {

    companion object {

        private const val KEY_NOTIFICATION_ACTION = "notificationAction"

        private fun newIntent(notificationAction: NotificationAction) = Intent(
                MyApplication.context,
                NotificationActionService::class.java
        ).apply { putExtra(KEY_NOTIFICATION_ACTION, notificationAction) }

        /** todo written 2020-9-17, give it two months?
         * This is to assess whether or not a ForegroundService is needed for performing
         * NotificationActions.  For now, an IntentService will be used in production.  In proto, a
         * BroadcastReciver will be used, with temporary notifications.  If the temporary
         * notifications are all canceled on time (aren't left hanging before the NotificationAction
         * is complete), that means that BroadcastReceivers are safe to use.  If not, suck it up and
         * make a ForegroundService.
         */
        fun newPendingIntent(notificationAction: NotificationAction): PendingIntent {
            return if (NotificationWrapperImpl.showTemporary) {
                NotificationActionReceiver.newPendingIntent(notificationAction)
            } else {
                PendingIntent.getService(
                        MyApplication.context,
                        notificationAction.requestCode,
                        newIntent(notificationAction),
                        PendingIntent.FLAG_UPDATE_CURRENT
                )!!
            }
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        val notificationAction = intent!!.getParcelableExtra<NotificationAction>(KEY_NOTIFICATION_ACTION)!!

        notificationAction.perform()
    }
}