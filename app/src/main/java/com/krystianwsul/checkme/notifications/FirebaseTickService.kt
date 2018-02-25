package com.krystianwsul.checkme.notifications

import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService

class FirebaseTickService : JobService() {

    override fun onStartJob(job: JobParameters): Boolean {
        TickJobIntentService.tick(false, "FirebaseTickService")

        return false // todo change to true if firebase wasn't connectd
    }

    override fun onStopJob(job: JobParameters): Boolean {
        return false // todo
    }
}