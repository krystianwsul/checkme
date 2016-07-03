package com.krystianwsul.checkme.gui.tasks;

import java.util.ArrayList;

interface ScheduleFragment {
    boolean createRootTask(String name);

    boolean updateRootTask(int rootTaskId, String name);

    boolean createRootJoinTask(String name, ArrayList<Integer> joinTaskIds);
    boolean dataChanged();
}
