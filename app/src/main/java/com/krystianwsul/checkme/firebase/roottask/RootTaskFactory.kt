package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.UserKeyStore
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.merge

class RootTaskFactory(
        rootTaskLoader: RootTaskLoader,
        private val rootTaskUserCustomTimeProviderSource: RootTaskUserCustomTimeProviderSource,
        private val userKeyStore: UserKeyStore,
        private val rootTaskToRootTaskCoordinator: RootTaskToRootTaskCoordinator,
        domainDisposable: CompositeDisposable,
        private val rootTaskKeySource: RootTaskKeySource,
) : RootTask.Parent {

    private val rootTaskMap = mutableMapOf<TaskKey.Root, RootTask>()

    val rootTasks: Map<TaskKey.Root, RootTask> get() = rootTaskMap

    val changeTypes: Observable<ChangeType>

    init {
        val addChangeEventChangeTypes = rootTaskLoader.addChangeEvents
                .switchMapSingle { (taskRecord) ->
                    check(!rootTaskMap.containsKey(taskRecord.taskKey))

                    Singles.zip(
                            rootTaskToRootTaskCoordinator.getRootTasks(taskRecord).toSingleDefault(Unit),
                            rootTaskUserCustomTimeProviderSource.getUserCustomTimeProvider(taskRecord),
                    ).map { (_, userCustomTimeProvider) ->
                        RootTask(taskRecord, this, userCustomTimeProvider)
                    }
                }
                .doOnNext {
                    check(!rootTaskMap.containsKey(it.taskKey))

                    rootTaskMap[it.taskKey] = it
                }

        val removeEventChangeTypes = rootTaskLoader.removeEvents.doOnNext {
            it.taskKeys.forEach {
                check(rootTaskMap.containsKey(it))

                rootTaskMap.remove(it)
            }

            userKeyStore.onTasksRemoved(it.taskKeys)
            rootTaskKeySource.onRootTasksRemoved(it.taskKeys)
        }

        changeTypes = listOf(addChangeEventChangeTypes, removeEventChangeTypes).merge()
                .map { ChangeType.REMOTE }
                .publishImmediate(domainDisposable)
    }

    override fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<TaskHierarchy> {
        return rootTasks.flatMap { it.value.nestedParentTaskHierarchies.values }
                .filter { it.parentTaskKey == parentTaskKey }
                .toSet()
    }

    override fun deleteTask(task: RootTask) {
        TODO("todo task edit")
    }

    override fun getTask(taskKey: TaskKey.Root) = rootTasks.getValue(taskKey)

    override fun getProject(projectId: String): Project<*> {
        TODO("todo task project")
    }
}