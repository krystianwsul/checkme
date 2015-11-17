package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.groups.Group;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;

import java.util.ArrayList;

/**
 * Created by Krystian on 11/2/2015.
 */
public interface Instance {
    String getName();
    String getScheduleText(Context context);
    ArrayList<Instance> getChildInstances();
    String getIntentKey();
    int getIntentValue();
    DateTime getDateTime();
    TimeStamp getDone();
    void setDone(boolean done);
}
