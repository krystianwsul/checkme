package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/17/2015.
 */
public interface Schedule {
    ArrayList<DateTime> getDateTimes(TimeStamp startTimeStamp, TimeStamp endTimeStamp);
    String getTaskText(Context context);
}
