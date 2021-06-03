package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getShowNotificationGroupData
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.common.utils.InstanceKey
import io.reactivex.rxjava3.kotlin.plusAssign

class ShowNotificationGroupViewModel : DomainViewModel<ShowNotificationGroupViewModel.Data>() {

    private lateinit var instanceKeys: Set<InstanceKey>

    private var cachedInstanceKeys = emptySet<InstanceKey>()

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) =
            domainFactory.getShowNotificationGroupData(instanceKeys + cachedInstanceKeys)
    }

    init {
        clearedDisposable += data.subscribe {
            cachedInstanceKeys = it.groupListDataWrapper
                .instanceDatas
                .map { it.instanceKey }
                .toSet()
        }
    }

    fun start(instanceKeys: Set<InstanceKey>) {
        check(instanceKeys.isNotEmpty())

        this.instanceKeys = instanceKeys

        internalStart()
    }

    data class Data(val groupListDataWrapper: GroupListDataWrapper) : DomainData()
}