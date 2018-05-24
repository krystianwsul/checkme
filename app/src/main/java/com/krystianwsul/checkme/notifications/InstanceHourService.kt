package com.krystianwsul.checkme.notifications

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Parcelable

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.NotificationWrapper
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey

import junit.framework.Assert

class InstanceHourService : IntentService("InstanceHourService") {

    companion object {

        private const val INSTANCE_KEY = "instanceKey"
        private const val NOTIFICATION_ID_KEY = "notificationId"

        fun getIntent(context: Context, instanceKey: InstanceKey, notificationId: Int) = Intent(context, InstanceHourService::class.java).apply {
            putExtra(INSTANCE_KEY, instanceKey as Parcelable)
            putExtra(NOTIFICATION_ID_KEY, notificationId)
        }
    }

    override fun onHandleIntent(intent: Intent) {
        Assert.assertTrue(intent.hasExtra(INSTANCE_KEY))
        Assert.assertTrue(intent.hasExtra(NOTIFICATION_ID_KEY))

        val instanceKey = intent.getParcelableExtra<InstanceKey>(INSTANCE_KEY)!!

        val notificationId = intent.getIntExtra(NOTIFICATION_ID_KEY, -1)
        Assert.assertTrue(notificationId != -1)

        val notificationWrapper = NotificationWrapper.instance
        notificationWrapper.cleanGroup(notificationId)

        InstanceDoneService.throttleFirebase(this, instanceKey.type == TaskKey.Type.REMOTE, { setInstanceAddHour(it, instanceKey) })
    }

    private fun setInstanceAddHour(domainFactory: DomainFactory, instanceKey: InstanceKey) = domainFactory.setInstanceAddHourService(this, SaveService.Source.SERVICE, instanceKey)
}
