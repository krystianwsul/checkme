package com.krystianwsul.checkme.notifications

import android.util.Log
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.krystianwsul.checkme.domainmodel.DomainFactory

class FirebaseTickService : JobService() {

    var running = false

    override fun onStartJob(job: JobParameters): Boolean {
        Log.e("asdf", "start");
        running = TickJobIntentService.tick(false, "FirebaseTickService", DomainFactory.TickData.Listener {
            Log.e("asdf", "callback");
            running = false // todo callback doesn't seem to be working
        })

        return running
    }

    override fun onStopJob(job: JobParameters): Boolean = running
}