package com.krystianwsul.checkme.loaders

import android.content.Context
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.utils.TaskKey


class ShowTaskLoader(context: Context, private val taskKey: TaskKey) : DomainLoader<ShowTaskLoader.DomainData>(context, if (taskKey.type == TaskKey.Type.REMOTE) FirebaseLevel.NEED else FirebaseLevel.NOTHING) {

    override val name = "ShowTaskLoader, taskKey: $taskKey"

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getShowTaskData(taskKey, context)

    data class DomainData(val name: String, val scheduleText: String?, val taskData: TaskListFragment.TaskData, val hasInstances: Boolean) : com.krystianwsul.checkme.loaders.DomainData() {

        init {
            check(name.isNotEmpty())
        }
    }
}
