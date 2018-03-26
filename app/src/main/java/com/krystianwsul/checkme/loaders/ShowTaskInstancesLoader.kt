package com.krystianwsul.checkme.loaders

import android.content.Context
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.TaskKey

class ShowTaskInstancesLoader(context: Context, private val taskKey: TaskKey) : DomainLoader<ShowTaskInstancesLoader.Data>(context, needsFirebase(taskKey)) {

    companion object {

        private fun needsFirebase(taskKey: TaskKey): DomainLoader.FirebaseLevel {
            return if (taskKey.type == TaskKey.Type.REMOTE)
                DomainLoader.FirebaseLevel.NEED
            else
                DomainLoader.FirebaseLevel.NOTHING
        }
    }

    override val name = "ShowTaskInstancesLoader, taskKey: $taskKey"

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getShowTaskInstancesData(taskKey)

    data class Data(val dataWrapper: GroupListFragment.DataWrapper) : DomainLoader.Data()
}
