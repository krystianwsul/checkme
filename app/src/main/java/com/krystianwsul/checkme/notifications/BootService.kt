package com.krystianwsul.checkme.notifications

import android.content.Intent
import android.support.v4.app.JobIntentService
import com.krystianwsul.checkme.MyApplication

class BootService : JobIntentService() {

    companion object {

        fun start() {
            JobIntentService.enqueueWork(MyApplication.instance, BootService::class.java, 1, Intent())
        }
    }

    override fun onHandleWork(intent: Intent) {
        TickService.tick(this, true, "BootService")
    }
}