package com.example.krystianwsul.organizatortest.gui.tasks;

import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Schedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;

public interface ScheduleFragment {
    boolean isValidTime();
    Schedule createSchedule(Task rootTask, TimeStamp startTimeStamp);
}
