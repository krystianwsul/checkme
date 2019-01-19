package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.InstanceKey

class ShowNotificationGroupViewModel : DomainViewModel<ShowNotificationGroupViewModel.Data>() {

    private lateinit var instanceKeys: Set<InstanceKey>

    fun start(instanceKeys: Set<InstanceKey>) {
        check(!instanceKeys.isEmpty())

        this.instanceKeys = instanceKeys

        internalStart()
    }

    override fun getData() = domainFactory.getShowNotificationGroupData(instanceKeys)

    data class Data(val dataWrapper: GroupListFragment.DataWrapper) : DomainData()
}