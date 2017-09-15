package com.krystianwsul.checkme.notifications

import com.google.android.gms.gcm.GcmNetworkManager
import com.google.android.gms.gcm.GcmTaskService
import com.google.android.gms.gcm.TaskParams

class GcmTickService : GcmTaskService() {

    override fun onRunTask(taskParams: TaskParams): Int {
        TickService.tick(this, false, "GcmTickService")

        return GcmNetworkManager.RESULT_SUCCESS
    }
}