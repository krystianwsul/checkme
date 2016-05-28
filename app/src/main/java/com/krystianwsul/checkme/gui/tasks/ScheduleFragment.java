package com.krystianwsul.checkme.gui.tasks;

import java.util.ArrayList;

interface ScheduleFragment {
    void createRootTask(String name);
    void updateRootTask(int rootTaskId, String name);
    void createRootJoinTask(String name, ArrayList<Integer> joinTaskIds);
}
