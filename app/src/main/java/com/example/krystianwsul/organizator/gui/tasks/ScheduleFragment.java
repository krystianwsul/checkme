package com.example.krystianwsul.organizator.gui.tasks;

import java.util.ArrayList;

interface ScheduleFragment {
    boolean isValidTime();
    void createRootTask(String name);
    void updateRootTask(int rootTaskId, String name);
    void createRootJoinTask(String name, ArrayList<Integer> joinTaskIds);
}
