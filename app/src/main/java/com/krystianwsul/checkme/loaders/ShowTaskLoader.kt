package com.krystianwsul.checkme.loaders

import android.content.Context
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.utils.TaskKey


class ShowTaskLoader(context: Context, private val taskKey: TaskKey) : DomainLoader<ShowTaskLoader.Data>(context, if (taskKey.type == TaskKey.Type.REMOTE) DomainLoader.FirebaseLevel.NEED else DomainLoader.FirebaseLevel.NOTHING) {

    override val name = "ShowTaskLoader, taskKey: $taskKey"

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getShowTaskData(taskKey, context)

    data class Data(val name: String, val scheduleText: String?, val taskData: TaskListFragment.TaskData, val hasInstances: Boolean) : DomainLoader.Data() {

        init {
            check(name.isNotEmpty())
        }
    }
}
