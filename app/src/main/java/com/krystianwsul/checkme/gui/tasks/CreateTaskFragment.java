package com.krystianwsul.checkme.gui.tasks;

import java.util.List;

public interface CreateTaskFragment {
    boolean updateTask(int taskId, String name);

    boolean createJoinTask(String name, List<Integer> taskIds);

    boolean createTask(String name);

    boolean dataChanged();
}
