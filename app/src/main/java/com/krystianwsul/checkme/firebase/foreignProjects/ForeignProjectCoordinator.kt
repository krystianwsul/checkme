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

        private var taskProjectKeys = setOf<ProjectKey<*>>()

        private var taskQueueState: TaskQueueState = TaskQueueState.Valid

        override fun onTaskAdded(task: RootTask) {
            taskQueueState = taskQueueState.addTask(task)
        }

        override fun onTaskChanged() {
            taskQueueState = TaskQueueState.Invalidated
        }

        override fun onTaskRemoved() {
            taskQueueState = TaskQueueState.Invalidated
        }

        private fun processTaskQueueState() {
            when (val taskQueueState = taskQueueState) {
                TaskQueueState.Valid -> {}
                is TaskQueueState.TasksAdded -> taskProjectKeys += taskQueueState.tasks.mapNotNull { it.projectKey }
                TaskQueueState.Invalidated -> taskProjectKeys = getAllTasks().mapNotNull { it.projectKey }.toSet()
            }

            taskQueueState = TaskQueueState.Valid
        }

        private sealed class TaskQueueState {

            abstract fun addTask(task: RootTask): TaskQueueState

            object Valid : TaskQueueState() {

                override fun addTask(task: RootTask) = TasksAdded(task)
            }

            class TasksAdded(task: RootTask) : TaskQueueState() {

                val tasks = mutableListOf(task) // todo projectKey convert to set later

                override fun addTask(task: RootTask): TaskQueueState {
                    tasks += task

                    return this
                }
            }

            object Invalidated : TaskQueueState() {

                override fun addTask(task: RootTask) = this
            }
        }
    }
}