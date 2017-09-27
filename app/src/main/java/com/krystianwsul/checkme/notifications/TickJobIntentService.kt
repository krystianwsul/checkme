package com.krystianwsul.checkme.notifications

import android.content.Intent
import android.support.v4.app.JobIntentService
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.persistencemodel.SaveService

class TickJobIntentService : JobIntentService() {

    companion object {

        fun start(intent: Intent) {
            JobIntentService.enqueueWork(MyApplication.instance, TickJobIntentService::class.java, 1, intent)
        }
    }

    override fun onHandleWork(intent: Intent) {
        TickService.tick(SaveService.Source.SERVICE, intent)
    }
}