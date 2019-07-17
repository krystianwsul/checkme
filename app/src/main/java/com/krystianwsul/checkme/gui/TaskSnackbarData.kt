package com.krystianwsul.checkme.gui

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.persistencemodel.SaveService

class TaskSnackbarData(private val taskUndoData: DomainFactory.TaskUndoData) : SnackbarData {

    override fun show(snackbarListener: SnackbarListener) {
        snackbarListener.showSnackbarRemoved(1) {
            DomainFactory.instance.clearTaskEndTimeStamps(SaveService.Source.GUI, taskUndoData)
        }
    }
}