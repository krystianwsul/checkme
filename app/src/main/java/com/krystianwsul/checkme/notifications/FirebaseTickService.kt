package com.krystianwsul.checkme.notifications

import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.TickData

class FirebaseTickService : JobService() {

    var running = false

    override fun onStartJob(job: JobParameters): Boolean {
        running = TickJobIntentService.tick(false, "FirebaseTickService", TickData.Listener {
            running = false
        })

        return running
    }

    override fun onStopJob(job: JobParameters): Boolean {
        if (!DomainFactory.getDomainFactory().isHoldingWakeLock)
            return false

        return running
    }
}