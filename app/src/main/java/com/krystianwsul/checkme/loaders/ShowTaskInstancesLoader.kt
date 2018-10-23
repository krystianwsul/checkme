package com.krystianwsul.checkme.loaders

import android.content.Context
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.viewmodels.FirebaseLevel

class ShowTaskInstancesLoader(context: Context, private val taskKey: TaskKey) : DomainLoader<ShowTaskInstancesLoader.DomainData>(context, needsFirebase(taskKey)) {

    companion object {

        private fun needsFirebase(taskKey: TaskKey): FirebaseLevel {
            return if (taskKey.type == TaskKey.Type.REMOTE)
                FirebaseLevel.NEED
            else
                FirebaseLevel.NOTHING
        }
    }

    override val name = "ShowTaskInstancesLoader, taskKey: $taskKey"

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getShowTaskInstancesData(taskKey)

    data class DomainData(val dataWrapper: GroupListFragment.DataWrapper) : com.krystianwsul.checkme.viewmodels.DomainData()
}
