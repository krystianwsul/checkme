package com.krystianwsul.checkme.notifications

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceNotificationDone
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.utils.InstanceKey

class InstanceDoneService : IntentService("InstanceDoneService") {

    companion object {

        private const val INSTANCE_KEY = "instanceKey"
        private const val NOTIFICATION_ID_KEY = "notificationId"
        private const val KEY_NAME = "name"

        fun getIntent(
            context: Context,
            instanceKey: InstanceKey,
            notificationId: Int,
            name: String
        ) = Intent(context, InstanceDoneService::class.java).apply {
            putExtra(INSTANCE_KEY, instanceKey as Parcelable)
            putExtra(NOTIFICATION_ID_KEY, notificationId)
            putExtra(KEY_NAME, name)
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        Preferences.tickLog.logLineDate("InstanceDoneService.onHandleIntent")

        check(intent!!.hasExtra(INSTANCE_KEY))
        check(intent.hasExtra(NOTIFICATION_ID_KEY))

        val instanceKey = intent.getParcelableExtra<InstanceKey>(INSTANCE_KEY)!!

        val notificationId = intent.getIntExtra(NOTIFICATION_ID_KEY, -1)
        check(notificationId != -1)

        val name = intent.getStringExtra(KEY_NAME)!!

        val notificationWrapper = NotificationWrapper.instance
        notificationWrapper.cleanGroup(notificationId)

        DomainFactory.addFirebaseListener("InstanceDoneService $name") { it.setInstanceNotificationDone(SaveService.Source.SERVICE, instanceKey) }
    }
}
