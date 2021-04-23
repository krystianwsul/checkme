package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.utils.TaskKey

class RootTaskFactory(
        private val rootTaskLoader: RootTaskLoader,
) {

    private val rootTasks = mutableMapOf<TaskKey.Root, RootTask>()

    /*
    val changeTypes: Observable<ChangeType>

    init {
        rootTaskLoader.addChangeEvents
                .doOnNext { (taskKey, taskRecord) ->
                    check(!rootTasks.containsKey(taskKey))

                    val task = RootTask()
                }



    }*/
}