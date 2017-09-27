package com.krystianwsul.checkme.notifications

import com.google.android.gms.gcm.GcmNetworkManager
import com.google.android.gms.gcm.GcmTaskService
import com.google.android.gms.gcm.TaskParams
import com.krystianwsul.checkme.persistencemodel.SaveService

class GcmTickService : GcmTaskService() {

    override fun onRunTask(taskParams: TaskParams): Int {
        TickService.tick(SaveService.Source.SERVICE, false, "GcmTickService")

        return GcmNetworkManager.RESULT_SUCCESS
    }
}