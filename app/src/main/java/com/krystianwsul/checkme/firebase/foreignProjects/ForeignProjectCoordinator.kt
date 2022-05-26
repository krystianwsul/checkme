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
     */

    fun onTaskAdded(task: RootTask)

    fun onTaskChanged()

    fun onTaskRemoved()
}