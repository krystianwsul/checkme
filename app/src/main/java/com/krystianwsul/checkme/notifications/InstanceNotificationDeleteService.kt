package com.krystianwsul.checkme.notifications

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceNotified
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.utils.InstanceKey

class InstanceNotificationDeleteService : IntentService("InstanceNotificationDeleteService") {

    companion object {

        private const val INSTANCE_KEY = "instanceKey"

        fun getIntent(context: Context, instanceKey: InstanceKey) = Intent(context, InstanceNotificationDeleteService::class.java).apply {
            putExtra(INSTANCE_KEY, instanceKey as Parcelable)
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        val instanceKey = intent!!.getParcelableExtra<InstanceKey>(INSTANCE_KEY)!!

        DomainFactory.addFirebaseListener {
            it.checkSave()
            it.setInstanceNotified(0, SaveService.Source.SERVICE, instanceKey)
        }
    }
}
