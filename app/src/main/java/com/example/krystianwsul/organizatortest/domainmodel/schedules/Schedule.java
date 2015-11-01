package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/17/2015.
 */
public interface Schedule {
    String getTaskText(Context context);
    ArrayList<Instance> getInstances(TimeStamp startTimeStamp, TimeStamp endTimeStamp);
}
