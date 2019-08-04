package com.krystianwsul.checkme.notifications

import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService

class FirebaseTickService : JobService() {

    private var running = true

    override fun onStartJob(job: JobParameters): Boolean {
        TickJobIntentService.tick(false, "FirebaseTickService") { running = false }

        return running
    }

    override fun onStopJob(job: JobParameters) = running
}