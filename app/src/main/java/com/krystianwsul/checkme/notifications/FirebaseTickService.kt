package com.krystianwsul.checkme.notifications

import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory

class FirebaseTickService : JobService() {

    private var running = false

    override fun onStartJob(job: JobParameters): Boolean {
        running = TickJobIntentService.tick(false, "FirebaseTickService") { running = false }

        return running
    }

    override fun onStopJob(job: JobParameters): Boolean {
        if (!KotlinDomainFactory.getKotlinDomainFactory().domainFactory.isHoldingWakeLock)
            return false

        return running
    }
}