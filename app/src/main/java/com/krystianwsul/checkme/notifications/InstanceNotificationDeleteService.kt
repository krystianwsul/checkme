package com.krystianwsul.checkme.notifications

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey

class InstanceNotificationDeleteService : IntentService("InstanceNotificationDeleteService") {

    companion object {

        private const val INSTANCE_KEY = "instanceKey"

        fun getIntent(context: Context, instanceKey: InstanceKey) = Intent(context, InstanceNotificationDeleteService::class.java).apply {
            if (instanceKey.type == TaskKey.Type.REMOTE && instanceKey.scheduleKey.scheduleTimePair.customTimeKey != null)
                check(instanceKey.scheduleKey.scheduleTimePair.customTimeKey is CustomTimeKey.RemoteCustomTimeKey)

            putExtra(INSTANCE_KEY, instanceKey as Parcelable)
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        val instanceKey = intent!!.getParcelableExtra<InstanceKey>(INSTANCE_KEY)!!

        DomainFactory.instance.setInstanceNotified(0, SaveService.Source.SERVICE, instanceKey)
    }
}
