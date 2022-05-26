package com.krystianwsul.checkme.firebase.foreignProjects

import com.krystianwsul.common.firebase.models.task.RootTask

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

    class Impl : ForeignProjectCoordinator {

        override fun onTaskAdded(task: RootTask) {
            // todo projectKey
        }

        override fun onTaskChanged() {
            // todo projectKey
        }

        override fun onTaskRemoved() {
            // todo projectKey
        }
    }
}