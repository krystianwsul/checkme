package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.common.utils.InstanceKey

class ShowNotificationGroupViewModel : DomainViewModel<ShowNotificationGroupViewModel.Data>() {

    private lateinit var instanceKeys: Set<InstanceKey>

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getShowNotificationGroupData(instanceKeys)
    }

    fun start(instanceKeys: Set<InstanceKey>) {
        check(instanceKeys.isNotEmpty())

        this.instanceKeys = instanceKeys

        internalStart()
    }

    data class Data(val groupListDataWrapper: GroupListDataWrapper) : DomainData()
}