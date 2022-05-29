package com.krystianwsul.checkme.firebase.foreignProjects

import com.krystianwsul.checkme.firebase.database.DatabaseResultQueue
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign

interface ForeignProjectCoordinator {

    /*
    All of these are for remote events.  In the meantime, a particular task's project may be edited locally - but only to a
    project that is already loaded.  So, every time we have a remote event that could potentially unload a project (change,
    remove), we need to re-evaluate all the loaded tasks.
     */

    val foreignProjectKeysObservable: Observable<Set<ProjectKey<*>>>

    fun onTaskAdded(task: RootTask)

    fun onTaskChanged()

    fun onTaskRemoved()

    class Impl(
        privateProjectKey: ProjectKey.Private,
        myUserFactorySingle: Single<MyUserFactory>,
        domainDisposable: CompositeDisposable,
        private val getAllTasks: () -> Collection<RootTask>,
    ) : ForeignProjectCoordinator {

        private var taskQueueState: TaskQueueState = TaskQueueState.Valid.initial()

        private var userProjectKeys = setOf<ProjectKey<*>>()

        override val foreignProjectKeysObservable = listOf(
            DatabaseResultQueue.onDequeued,
            myUserFactorySingle.flatMapObservable { myUserFactory ->
                myUserFactory.sharedProjectKeysObservable.doOnNext { userProjectKeys = it + privateProjectKey }
            },
        ).merge()
            .doOnNext { taskQueueState = taskQueueState.resolve(getAllTasks) }
            .map {
                taskQueueState.let { it as TaskQueueState.Valid }.taskProjectKeys - userProjectKeys
            }
            .replay(1)
            .also { domainDisposable += it.connect() }

        override fun onTaskAdded(task: RootTask) {
            taskQueueState = taskQueueState.addTask(task)
        }

        override fun onTaskChanged() {
            taskQueueState = TaskQueueState.Invalidated
        }

        override fun onTaskRemoved() {
            taskQueueState = TaskQueueState.Invalidated
        }

        private sealed class TaskQueueState {

            abstract fun addTask(task: RootTask): TaskQueueState

            abstract fun resolve(getAllTasks: () -> Collection<RootTask>): Valid

            class Valid(val taskProjectKeys: Set<ProjectKey<*>>) : TaskQueueState() {

                companion object {

                    fun initial() = Valid(setOf())
                }

                override fun addTask(task: RootTask) = TasksAdded(taskProjectKeys, task)

                override fun resolve(getAllTasks: () -> Collection<RootTask>) = this
            }

            class TasksAdded(private val oldTaskProjectKeys: Set<ProjectKey<*>>, task: RootTask) : TaskQueueState() {

                val tasks = mutableListOf(task)

                override fun addTask(task: RootTask): TaskQueueState {
                    tasks += task

                    return this
                }

                override fun resolve(getAllTasks: () -> Collection<RootTask>) =
                    Valid(oldTaskProjectKeys + tasks.distinct().mapNotNull { it.projectKey })
            }

            object Invalidated : TaskQueueState() {

                override fun addTask(task: RootTask) = this

                override fun resolve(getAllTasks: () -> Collection<RootTask>) =
                    Valid(getAllTasks().mapNotNull { it.projectKey }.toSet())
            }
        }
    }
}