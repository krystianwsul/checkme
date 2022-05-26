package com.krystianwsul.checkme.firebase.foreignProjects

import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.utils.ProjectKey

interface ForeignProjectCoordinator {

    /*
    All of these are for remote events.  In the meantime, a particular task's project may be edited locally - but only to a
    project that is already loaded.  So, every time we have a remote event that could potentially unload a project (change,
    remove), we need to re-evaluate all the loaded tasks.
     */

    /*
    todo projectKey: in the implementation, listen for DatabaseResultQueue.onDequeued.  In the meantime, queue operations:
    when a task is added, queue it up.  When a task is changed or removed, ignore task adds and just re-do everything
    on the next trigger.

    In the meantime, watch userFactorySingle over from FactoryLoader.  Grab the latest value from there somehow.  Every time
    that changes (that includes local changes too, which we do need), also treat that as a trigger.

    So we have our two triggers now.  Subtract the user's project keys from the total shared keys, add the private keys that
    aren't our own, and voila - emit the change.
     */

    fun onTaskAdded(task: RootTask)

    fun onTaskChanged()

    fun onTaskRemoved()

    class Impl(private val getAllTasks: () -> Collection<RootTask>) : ForeignProjectCoordinator {

        private var taskQueueState: TaskQueueState = TaskQueueState.Valid.initial()

        override fun onTaskAdded(task: RootTask) {
            taskQueueState = taskQueueState.addTask(task)
        }

        override fun onTaskChanged() {
            taskQueueState = TaskQueueState.Invalidated
        }

        override fun onTaskRemoved() {
            taskQueueState = TaskQueueState.Invalidated
        }

        private fun resolveTaskQueueState() {
            taskQueueState = taskQueueState.resolve(getAllTasks)
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