package com.example.krystianwsul.organizatortest.domain.tasks;

import com.example.krystianwsul.organizatortest.domain.instances.TopInstance;
import com.example.krystianwsul.organizatortest.timing.TimeStamp;
import com.example.krystianwsul.organizatortest.timing.schedules.Schedule;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/13/2015.
 */
public interface TopTask extends Task {
    Schedule getSchedule();
    ArrayList<TopInstance> getInstances(TimeStamp startTimeStamp, TimeStamp endTimeStamp);
}
