package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.common.utils.TaskKey

class ShowTaskInstancesViewModel : DomainViewModel<ShowTaskInstancesViewModel.Data>() {

    private lateinit var taskKey: TaskKey

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getShowTaskInstancesData(taskKey)
    }

    fun start(taskKey: TaskKey) {
        this.taskKey = taskKey

        internalStart()
    }

    data class Data(
            val dataWrapper: GroupListFragment.DataWrapper,
            val showLoader: Boolean
    ) : DomainData()
}