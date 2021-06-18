package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.extensions.getShowInstanceData
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey

class ShowInstanceViewModel : DomainViewModel<ShowInstanceViewModel.Data>() {

    private lateinit var instanceKey: InstanceKey

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData { it.getShowInstanceData(instanceKey) }
    }

    fun start(instanceKey: InstanceKey) {
        this.instanceKey = instanceKey

        internalStart()
    }

    data class Data(
            val name: String,
            val instanceDateTime: DateTime,
            val done: Boolean,
            val taskCurrent: Boolean,
            val isRootInstance: Boolean,
            val groupListDataWrapper: GroupListDataWrapper,
            val notificationShown: Boolean,
            val displayText: String,
            val taskKey: TaskKey,
            val isVisible: Boolean,
            val newInstanceKey: InstanceKey,
    ) : DomainData() {

        init {
            check(name.isNotEmpty())
        }
    }
}