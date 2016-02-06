package com.example.krystianwsul.organizator.gui.tasks;

import com.example.krystianwsul.organizator.domainmodel.Task;

import java.util.ArrayList;

interface ScheduleFragment {
    boolean isValidTime();
    void createRootTask(String name);
    void updateRootTask(Task rootTask, String name);
    void createRootJoinTask(String name, ArrayList<Task> joinTasks);
}
