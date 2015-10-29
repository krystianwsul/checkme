package com.example.krystianwsul.organizatortest.domainmodel.instances;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;

/**
 * Created by Krystian on 10/23/2015.
 */
public interface TopInstance extends Instance, Comparable<TopInstance> {
    DateTime getDateTime();
    boolean hasChildren();
}
