package com.krystianwsul.checkme.gui.instances.edit

import com.krystianwsul.checkme.domainmodel.extensions.undo
import com.krystianwsul.checkme.domainmodel.undo.UndoData
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.base.SnackbarListener
import com.krystianwsul.common.time.TimeStamp
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

abstract class SnackbarEditInstancesHostDelegate(private val compositeDisposable: CompositeDisposable) :
    EditInstancesHostDelegate() {

    protected abstract val snackbarListener: SnackbarListener

    override fun afterEditInstances(undoData: UndoData, count: Int, newTimeStamp: TimeStamp?) {
        snackbarListener.showSnackbarHourMaybe(count)
            .flatMapCompletable { AndroidDomainUpdater.undo(notificationType, undoData) }
            .subscribe()
            .addTo(compositeDisposable)
    }
}