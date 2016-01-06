package com.example.krystianwsul.organizator.gui.tasks;

import com.example.krystianwsul.organizator.domainmodel.Schedule;
import com.example.krystianwsul.organizator.domainmodel.Task;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

interface ScheduleFragment {
    boolean isValidTime();
    Schedule createSchedule(Task rootTask, TimeStamp startTimeStamp);
}
