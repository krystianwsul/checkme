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
                .doOnNext { (taskRecord, projectKey) ->
                    check(!rootTasks.containsKey(taskRecord.taskKey))

                    val task = RootTask(
                            projectKey,
                            taskRecord,
                            this,

                    )
                }



    }*/
}