package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.UserKeyStore
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.taskhierarchy.NestedTaskHierarchy
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge

class RootTaskFactory(
        private val rootTaskLoader: RootTaskLoader,
        private val rootTaskUserCustomTimeProviderSource: RootTaskUserCustomTimeProviderSource,
        private val userKeyStore: UserKeyStore,
        private val rootTaskCoordinator: RootTaskCoordinator,
        private val domainDisposable: CompositeDisposable,
        private val rootTaskKeySource: RootTaskKeySource,
) : RootTask.Parent {

    private val rootTasks = mutableMapOf<TaskKey.Root, RootTask>()

    val changeTypes: Observable<ChangeType>

    init {
        val addChangeEventChangeTypes = rootTaskLoader.addChangeEvents
                .switchMapSingle { (taskRecord, projectKey) ->
                    check(!rootTasks.containsKey(taskRecord.taskKey))

                    rootTaskUserCustomTimeProviderSource.getUserCustomTimeProvider(taskRecord).map {
                        RootTask(projectKey, taskRecord, this, it)
                    }
                    // todo task fetch block for child tasks
                }
                .doOnNext {
                    check(!rootTasks.containsKey(it.taskKey))

                    rootTasks[it.taskKey] = it
                }

        val removeEventChangeTypes = rootTaskLoader.removeEvents.doOnNext {
            it.taskKeys.forEach { check(rootTasks.containsKey(it)) }

            userKeyStore.onTasksRemoved(it.taskKeys)
            // todo task fetch update root task key source

            it.taskKeys.forEach { rootTasks.remove(it) }
        }

        changeTypes = listOf(addChangeEventChangeTypes, removeEventChangeTypes).merge()
                .map { ChangeType.REMOTE }
                .publishImmediate(domainDisposable)
    }

    override fun getTaskHierarchiesByParentTaskKey(childTaskKey: TaskKey.Root): Set<NestedTaskHierarchy> {
        TODO("todo task after fetch")
    }

    override fun deleteTask(task: RootTask) {
        TODO("todo task after fetch")
    }

    override fun getTask(taskKey: TaskKey.Root): RootTask {
        TODO("todo task after fetch")
    }

    override fun getProject(projectKey: ProjectKey<*>): Project<*> {
        TODO("todo task after fetch")
    }
}