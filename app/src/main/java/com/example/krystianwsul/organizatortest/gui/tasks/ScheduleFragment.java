package com.example.krystianwsul.organizatortest.gui.tasks;

import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Schedule;

public interface ScheduleFragment {
    boolean isValidTime();
    Schedule createSchedule(RootTask rootTask);
}
