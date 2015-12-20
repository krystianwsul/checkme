package com.example.krystianwsul.organizator.gui.tasks;

import com.example.krystianwsul.organizator.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizator.domainmodel.tasks.Schedule;
import com.example.krystianwsul.organizator.domainmodel.tasks.Task;

public interface ScheduleFragment {
    boolean isValidTime();
    Schedule createSchedule(Task rootTask, TimeStamp startTimeStamp);
}
