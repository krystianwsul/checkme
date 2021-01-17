package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getShowTaskInstancesData
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.common.utils.TaskKey

class ShowTaskInstancesViewModel : DomainViewModel<ShowTaskInstancesViewModel.Data>() {

    private lateinit var taskKey: TaskKey
    private var page: Int = 0

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getShowTaskInstancesData(taskKey, page)
    }

    fun start(taskKey: TaskKey, page: Int) {
        this.taskKey = taskKey

        if (this.page != page) {
            this.page = page

            refresh()
        } else {
            internalStart()
        }
    }

    data class Data(
            val groupListDataWrapper: GroupListDataWrapper,
            val showLoader: Boolean,
    ) : DomainData()
}