package com.example.krystianwsul.organizatortest.timing.schedules;

import com.example.krystianwsul.organizatortest.timing.DateTime;
import com.example.krystianwsul.organizatortest.timing.TimeStamp;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/17/2015.
 */
public interface Schedule {
    ArrayList<DateTime> getDateTimes(TimeStamp startTimeStamp, TimeStamp endTimeStamp);
}
