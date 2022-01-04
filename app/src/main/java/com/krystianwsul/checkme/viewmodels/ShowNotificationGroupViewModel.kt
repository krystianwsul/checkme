package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.extensions.getShowNotificationGroupData
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.rxjava3.kotlin.plusAssign

class ShowNotificationGroupViewModel : DomainViewModel<ShowNotificationGroupViewModel.Data>() {

    private var projectKey: ProjectKey.Shared? = null

    private var cachedInstanceKeys = emptySet<InstanceKey>()

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher =
            DomainResultFetcher.DomainFactoryData { it.getShowNotificationGroupData(cachedInstanceKeys) }
    }

    init {
        clearedDisposable += data.subscribe {
            cachedInstanceKeys = it.groupListDataWrapper
                .allInstanceDatas
                .map { it.instanceKey }
                .toSet()
        }
    }

    fun start(projectKey: ProjectKey.Shared?) {
        this.projectKey = projectKey

        internalStart()
    }

    data class Data(val groupListDataWrapper: GroupListDataWrapper) : DomainData()
}