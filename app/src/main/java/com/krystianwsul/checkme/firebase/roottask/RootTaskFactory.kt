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
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.merge

class RootTaskFactory(
        private val rootTaskLoader: RootTaskLoader,
        private val rootTaskUserCustomTimeProviderSource: RootTaskUserCustomTimeProviderSource,
        private val userKeyStore: UserKeyStore,
        private val rootTaskToRootTaskCoordinator: RootTaskToRootTaskCoordinator,
        private val domainDisposable: CompositeDisposable,
        private val rootTaskKeySource: RootTaskKeySource,
) : RootTask.Parent {

    private val rootTasks = mutableMapOf<TaskKey.Root, RootTask>()

    val changeTypes: Observable<ChangeType>

    init {
        val addChangeEventChangeTypes = rootTaskLoader.addChangeEvents
                .switchMapSingle { (taskRecord, projectKey) ->
                    check(!rootTasks.containsKey(taskRecord.taskKey))

                    Singles.zip(
                            rootTaskToRootTaskCoordinator.getRootTasks(taskRecord).toSingleDefault(Unit),
                            rootTaskUserCustomTimeProviderSource.getUserCustomTimeProvider(taskRecord),
                    ).map { (_, userCustomTimeProvider) ->
                        RootTask(projectKey, taskRecord, this, userCustomTimeProvider)
                    }
                }
                .doOnNext {
                    check(!rootTasks.containsKey(it.taskKey))

                    rootTasks[it.taskKey] = it
                }

        val removeEventChangeTypes = rootTaskLoader.removeEvents.doOnNext {
            it.taskKeys.forEach {
                check(rootTasks.containsKey(it))

                rootTasks.remove(it)
            }

            userKeyStore.onTasksRemoved(it.taskKeys)
            rootTaskKeySource.onRootTasksRemoved(it.taskKeys)
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