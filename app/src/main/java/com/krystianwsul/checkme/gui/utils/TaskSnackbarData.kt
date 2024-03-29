package com.krystianwsul.checkme.gui.utils

import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.clearTaskEndTimeStamps
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.base.SnackbarListener
import com.krystianwsul.common.domain.TaskUndoData

class TaskSnackbarData(private val taskUndoData: TaskUndoData) : SnackbarData {

    override fun show(snackbarListener: SnackbarListener) =
            snackbarListener.showSnackbarRemovedMaybe(1).flatMapCompletable {
                AndroidDomainUpdater.clearTaskEndTimeStamps(DomainListenerManager.NotificationType.All, taskUndoData)
            }!!
}