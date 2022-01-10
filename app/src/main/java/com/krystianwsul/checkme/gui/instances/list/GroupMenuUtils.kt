package com.krystianwsul.checkme.gui.instances.list

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.setInstancesAddHourActivity
import com.krystianwsul.checkme.domainmodel.extensions.setInstancesDone
import com.krystianwsul.checkme.domainmodel.extensions.setInstancesNotNotified
import com.krystianwsul.checkme.domainmodel.extensions.undoInstancesAddHour
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.base.SnackbarListener
import com.krystianwsul.checkme.gui.instances.edit.SnackbarEditInstancesHostDelegate
import com.krystianwsul.checkme.viewmodels.DataId
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.Disposable

typealias SelectedDatas = Collection<GroupListDataWrapper.SelectedData>

object GroupMenuUtils {

    fun showHour(selectedData: GroupListDataWrapper.SelectedData) = selectedData.run {
        this is GroupListDataWrapper.InstanceData && isRootInstance && done == null && instanceTimeStamp <= TimeStamp.now
    }

    private fun GroupListDataWrapper.SelectedData.showNotification() =
        this is GroupListDataWrapper.InstanceData && showHour(this)

    fun showNotification(selectedDatas: SelectedDatas) = selectedDatas.any { it.showNotification() }

    fun showHour(selectedDatas: SelectedDatas) = selectedDatas.all { showHour(it) }

    fun showEdit(selectedData: GroupListDataWrapper.SelectedData) =
        selectedData.run { this is GroupListDataWrapper.InstanceData && done == null }

    fun showEdit(selectedDatas: SelectedDatas) =
        selectedDatas.all { showEdit(it) }

    fun showCheck(selectedDatas: SelectedDatas) = showEdit(selectedDatas)

    fun showCheck(selectedData: GroupListDataWrapper.SelectedData) = showEdit(selectedData)

    fun showUncheck(selectedDatas: SelectedDatas) =
        selectedDatas.all { it is GroupListDataWrapper.InstanceData && it.done != null }

    @CheckResult
    fun onNotify(selectedDatas: SelectedDatas, dataId: DataId): Disposable {
        val instanceDatas = selectedDatas.filterIsInstance<GroupListDataWrapper.InstanceData>()
        check(instanceDatas.isNotEmpty())

        val instanceKeys = instanceDatas.map { it.instanceKey }

        return AndroidDomainUpdater.setInstancesNotNotified(
            DomainListenerManager.NotificationType.First(dataId),
            instanceKeys,
            false,
        ).subscribe()
    }

    @CheckResult
    fun onHour(
        selectedDatas: SelectedDatas,
        dataId: DataId,
        listener: SnackbarListener,
        beforeChangeHour: (() -> Unit)? = null,
        afterAddHour: ((newTimeStamp: TimeStamp) -> Unit)? = null,
        afterUndo: (() -> Unit)? = null,
    ): Disposable {
        beforeChangeHour?.invoke()

        val instanceKeys = selectedDatas.map { (it as GroupListDataWrapper.InstanceData).instanceKey }

        return AndroidDomainUpdater.setInstancesAddHourActivity(
            DomainListenerManager.NotificationType.First(dataId),
            instanceKeys,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { afterAddHour?.invoke(it.newTimeStamp) }
            .flatMapMaybe { listener.showSnackbarHourMaybe(it.instanceDateTimes.size).map { _ -> it } }
            .doOnSuccess { beforeChangeHour?.invoke() }
            .flatMapCompletable {
                AndroidDomainUpdater.undoInstancesAddHour(DomainListenerManager.NotificationType.First(dataId), it)
            }
            .doOnComplete { afterUndo?.invoke() }
            .subscribe()
    }

    fun onEdit(selectedDatas: SelectedDatas, snackbarHostDelegate: SnackbarEditInstancesHostDelegate) {
        check(selectedDatas.isNotEmpty())

        snackbarHostDelegate.show(selectedDatas.map { (it as GroupListDataWrapper.InstanceData).instanceKey })
    }

    private fun setInstancesDone(instanceKeys: List<InstanceKey>, done: Boolean, dataId: DataId) =
        AndroidDomainUpdater.setInstancesDone(DomainListenerManager.NotificationType.First(dataId), instanceKeys, done)

    @CheckResult
    fun onCheck(selectedDatas: SelectedDatas, dataId: DataId, listener: SnackbarListener): Disposable {
        val instanceKeys = selectedDatas.map { it as GroupListDataWrapper.InstanceData }
            .filter { it.done == null }
            .map { it.instanceKey }

        return setInstancesDone(instanceKeys, true, dataId).observeOn(AndroidSchedulers.mainThread())
            .andThen(Maybe.defer { listener.showSnackbarDoneMaybe(instanceKeys.size) })
            .flatMapCompletable { setInstancesDone(instanceKeys, false, dataId) }
            .subscribe()
    }

    @CheckResult
    fun onUncheck(selectedDatas: SelectedDatas, dataId: DataId, listener: SnackbarListener): Disposable {
        val instanceDatas = selectedDatas.map { it as GroupListDataWrapper.InstanceData }

        check(instanceDatas.all { it.done != null })

        val instanceKeys = instanceDatas.map { it.instanceKey }

        return setInstancesDone(instanceKeys, false, dataId).observeOn(AndroidSchedulers.mainThread())
            .andThen(Maybe.defer { listener.showSnackbarDoneMaybe(instanceKeys.size) })
            .flatMapCompletable { setInstancesDone(instanceKeys, true, dataId) }
            .subscribe()
    }
}