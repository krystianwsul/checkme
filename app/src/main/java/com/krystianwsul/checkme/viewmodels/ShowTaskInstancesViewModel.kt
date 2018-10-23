package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.TaskKey

class ShowTaskInstancesViewModel : DomainViewModel<ShowTaskInstancesViewModel.Data>() {

    private lateinit var taskKey: TaskKey

    fun start(taskKey: TaskKey) {
        this.taskKey = taskKey

        val firebaseLevel = if (taskKey.type == TaskKey.Type.REMOTE)
            FirebaseLevel.NEED
        else
            FirebaseLevel.NOTHING

        internalStart(firebaseLevel)
    }

    override fun getData(domainFactory: DomainFactory) = domainFactory.getShowTaskInstancesData(taskKey)

    data class Data(val dataWrapper: GroupListFragment.DataWrapper) : DomainData()
}