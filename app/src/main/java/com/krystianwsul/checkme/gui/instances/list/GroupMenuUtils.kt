package com.krystianwsul.checkme.gui.instances.list

import com.krystianwsul.common.time.TimeStamp

object GroupMenuUtils {

    private fun GroupListDataWrapper.SelectedData.showHour() =
        this is GroupListDataWrapper.InstanceData && isRootInstance && done == null && instanceTimeStamp <= TimeStamp.now

    private fun GroupListDataWrapper.SelectedData.showNotification() =
        this is GroupListDataWrapper.InstanceData && showHour() && !notificationShown

    fun showNotification(selectedDatas: Collection<GroupListDataWrapper.SelectedData>) =
        selectedDatas.any { it.showNotification() }

    fun showHour(selectedDatas: Collection<GroupListDataWrapper.SelectedData>) = selectedDatas.all { it.showHour() }

    fun showEdit(selectedDatas: Collection<GroupListDataWrapper.SelectedData>) = selectedDatas.all {
        it is GroupListDataWrapper.InstanceData && it.done == null
    }

    fun showCheck(selectedDatas: Collection<GroupListDataWrapper.SelectedData>) = showEdit(selectedDatas)

    fun showUncheck(selectedDatas: Collection<GroupListDataWrapper.SelectedData>) = selectedDatas.all {
        it is GroupListDataWrapper.InstanceData && it.done != null
    }
}