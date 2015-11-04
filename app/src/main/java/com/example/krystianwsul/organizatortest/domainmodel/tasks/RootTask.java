package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;

import java.util.ArrayList;

/**
 * Created by Krystian on 11/3/2015.
 */
public interface RootTask extends TaskTest {
    ArrayList<Instance> getInstances(TimeStamp startTimeStamp, TimeStamp endTimeStamp);
}
