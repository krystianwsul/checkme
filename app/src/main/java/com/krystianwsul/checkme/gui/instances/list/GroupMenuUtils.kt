package com.krystianwsul.checkme.gui.instances.list

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.setInstancesAddHourActivity
import com.krystianwsul.checkme.domainmodel.extensions.setInstancesDone
import com.krystianwsul.checkme.domainmodel.extensions.setInstancesNotNotified
import com.krystianwsul.checkme.domainmodel.extensions.undoInstancesAddHour
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.base.SnackbarListener
import com.krystianwsul.checkme.gui.instances.EditInstancesFragment
import com.krystianwsul.checkme.viewmodels.DataId
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.Disposable

typealias SelectedDatas = Collection<GroupListDataWrapper.SelectedData>

object GroupMenuUtils {

    private fun GroupListDataWrapper.SelectedData.showHour() =
        this is GroupListDataWrapper.InstanceData && isRootInstance && done == null && instanceTimeStamp <= TimeStamp.now

    private fun GroupListDataWrapper.SelectedData.showNotification() =
        this is GroupListDataWrapper.InstanceData && showHour() && !notificationShown

    fun showNotification(selectedDatas: SelectedDatas) =
        selectedDatas.any { it.showNotification() }

    fun showHour(selectedDatas: SelectedDatas) = selectedDatas.all { it.showHour() }

    fun showEdit(selectedDatas: SelectedDatas) =
        selectedDatas.all { it is GroupListDataWrapper.InstanceData && it.done == null }

    fun showCheck(selectedDatas: SelectedDatas) = showEdit(selectedDatas)

    fun showUncheck(selectedDatas: SelectedDatas) =
        selectedDatas.all { it is GroupListDataWrapper.InstanceData && it.done != null }

    @CheckResult
    fun onNotify(selectedDatas: SelectedDatas, dataId: DataId): Disposable {
        val instanceDatas =
            selectedDatas.filter { it.showNotification() }.map { it as GroupListDataWrapper.InstanceData }

        check(instanceDatas.isNotEmpty())

        val instanceKeys = instanceDatas.map { it.instanceKey }

        return AndroidDomainUpdater.setInstancesNotNotified(
            DomainListenerManager.NotificationType.First(dataId),
            instanceKeys,
        ).subscribe()
    }

    @CheckResult
    fun onHour(selectedDatas: SelectedDatas, dataId: DataId, listener: SnackbarListener): Disposable {
        check(showHour(selectedDatas))
        val instanceKeys = selectedDatas.map { (it as GroupListDataWrapper.InstanceData).instanceKey }

        return AndroidDomainUpdater.setInstancesAddHourActivity(
            DomainListenerManager.NotificationType.First(dataId),
            instanceKeys,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapMaybe { listener.showSnackbarHourMaybe(it.instanceDateTimes.size).map { _ -> it } }
            .flatMapCompletable {
                AndroidDomainUpdater.undoInstancesAddHour(
                    DomainListenerManager.NotificationType.First(dataId),
                    it,
                )
            }
            .subscribe()
    }

    fun onEdit(selectedDatas: SelectedDatas, snackbarHostDelegate: EditInstancesFragment.SnackbarHostDelegate) {
        check(selectedDatas.isNotEmpty())

        snackbarHostDelegate.show(selectedDatas.map { (it as GroupListDataWrapper.InstanceData).instanceKey })
    }

    private fun setInstancesDone(instanceKeys: List<InstanceKey>, done: Boolean, dataId: DataId) =
        AndroidDomainUpdater.setInstancesDone(DomainListenerManager.NotificationType.First(dataId), instanceKeys, done)

    @CheckResult
    fun onCheck(selectedDatas: SelectedDatas, dataId: DataId, listener: SnackbarListener): Disposable {
        val instanceDatas = selectedDatas.map { it as GroupListDataWrapper.InstanceData }

        check(instanceDatas.all { it.done == null })

        val instanceKeys = instanceDatas.map { it.instanceKey }

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