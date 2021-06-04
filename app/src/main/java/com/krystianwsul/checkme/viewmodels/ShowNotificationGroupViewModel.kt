package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getShowNotificationGroupData
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.rxjava3.kotlin.plusAssign

class ShowNotificationGroupViewModel : DomainViewModel<ShowNotificationGroupViewModel.Data>() {

    private var projectKey: ProjectKey.Shared? = null

    private var cachedInstanceKeys = emptySet<InstanceKey>()

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) =
            domainFactory.getShowNotificationGroupData(projectKey, cachedInstanceKeys)
    }

    init {
        clearedDisposable += data.subscribe {
            cachedInstanceKeys = it.groupListDataWrapper
                .instanceDatas
                .map { it.instanceKey }
                .toSet()
        }
    }

    fun start(projectKey: ProjectKey.Shared?) {
        this.projectKey = projectKey

        internalStart()
    }

    data class Data(val title: String?, val groupListDataWrapper: GroupListDataWrapper) : DomainData()
}