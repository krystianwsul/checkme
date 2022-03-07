package com.krystianwsul.checkme.firebase.dependencies

import com.krystianwsul.checkme.firebase.database.DatabaseResultEventSource
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Observable

class RootTaskKeyStore(databaseResultEventSource: DatabaseResultEventSource) {

    private val projectStore = RequestKeyStore<ProjectKey<*>, TaskKey.Root>()
    private val taskStore = RequestKeyStore<TaskKey.Root, TaskKey.Root>()

    private val requestMerger = RequestMerger(databaseResultEventSource, projectStore, taskStore)

    val rootTaskKeysObservable: Observable<Set<TaskKey.Root>> = requestMerger.outputObservable

    fun onProjectAddedOrUpdated(projectKey: ProjectKey<*>, rootTaskKeys: Set<TaskKey.Root>) = // todo queue
        projectStore.addRequest(projectKey, rootTaskKeys)

    fun onProjectsRemoved(projectKeys: Set<ProjectKey<*>>) = projectStore.onRequestsRemoved(projectKeys) // todo queue

    fun onRootTaskAddedOrUpdated(parentRootTaskKey: TaskKey.Root, childRootTaskKeys: Set<TaskKey.Root>) = // todo queue
        taskStore.addRequest(parentRootTaskKey, childRootTaskKeys)

    fun onRootTasksRemoved(rootTaskKeys: Set<TaskKey.Root>) = taskStore.onRequestsRemoved(rootTaskKeys) // todo queue

    sealed class ParentKey {

        data class Project(val projectKey: ProjectKey<*>) : ParentKey()
        data class Task(val taskKey: TaskKey.Root) : ParentKey()
    }
}