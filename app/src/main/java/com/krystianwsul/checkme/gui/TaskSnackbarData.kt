package com.krystianwsul.checkme.gui

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.clearTaskEndTimeStamps
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.domain.TaskUndoData

class TaskSnackbarData(private val taskUndoData: TaskUndoData) : SnackbarData {

    override fun show(snackbarListener: SnackbarListener) {
        snackbarListener.showSnackbarRemoved(1) {
            DomainFactory.instance.clearTaskEndTimeStamps(SaveService.Source.GUI, taskUndoData)
        }
    }
}