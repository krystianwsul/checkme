package com.krystianwsul.checkme.notifications

import android.app.IntentService
import android.content.Context
import android.content.Intent
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.utils.InstanceKey
import java.util.*

class GroupNotificationDeleteService : IntentService("GroupNotificationDeleteService") {

    companion object {

        private const val INSTANCES_KEY = "instanceKeys"

        fun getIntent(context: Context, instanceKeys: ArrayList<InstanceKey>) = Intent(context, GroupNotificationDeleteService::class.java).apply {
            check(instanceKeys.isNotEmpty())

            putParcelableArrayListExtra(INSTANCES_KEY, instanceKeys)
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        val instanceKeys = intent!!.getParcelableArrayListExtra<InstanceKey>(INSTANCES_KEY)!!
        check(instanceKeys.isNotEmpty())

        DomainFactory.addFirebaseListener { it.setInstancesNotified(SaveService.Source.SERVICE, instanceKeys) }
    }
}
