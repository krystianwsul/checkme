package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.RequestKeyStore
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Observable

class RootTaskKeySource {

    private val projectStore = RequestKeyStore<ProjectKey<*>, TaskKey.Root>()
    private val taskStore = RequestKeyStore<TaskKey.Root, TaskKey.Root>()

    val rootTaskKeysObservable: Observable<Set<TaskKey.Root>> = RequestKeyStore.merge(projectStore, taskStore)

    fun onProjectAddedOrUpdated(projectKey: ProjectKey<*>, rootTaskKeys: Set<TaskKey.Root>) =
            projectStore.addRequest(projectKey, rootTaskKeys)

    fun onProjectsRemoved(projectKeys: Set<ProjectKey<*>>) = projectStore.onRequestsRemoved(projectKeys)

    fun onRootTaskAddedOrUpdated(parentRootTaskKey: TaskKey.Root, childRootTaskKeys: Set<TaskKey.Root>) =
            taskStore.addRequest(parentRootTaskKey, childRootTaskKeys)

    fun onRootTasksRemoved(rootTaskKeys: Set<TaskKey.Root>) = taskStore.onRequestsRemoved(rootTaskKeys)

    sealed class ParentKey {

        data class Project(val projectKey: ProjectKey<*>) : ParentKey()
        data class Task(val taskKey: TaskKey.Root) : ParentKey()
    }
}