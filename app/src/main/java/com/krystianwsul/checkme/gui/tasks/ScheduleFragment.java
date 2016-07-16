package com.krystianwsul.checkme.gui.tasks;

import java.util.List;

interface ScheduleFragment {
    boolean createRootTask(String name);

    boolean updateRootTask(int rootTaskId, String name);

    boolean createRootJoinTask(String name, List<Integer> joinTaskIds);
    boolean dataChanged();
}
