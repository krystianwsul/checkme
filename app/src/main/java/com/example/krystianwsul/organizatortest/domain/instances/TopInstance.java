package com.example.krystianwsul.organizatortest.domain.instances;

import com.example.krystianwsul.organizatortest.timing.DateTime;

/**
 * Created by Krystian on 10/23/2015.
 */
public interface TopInstance extends Instance, Comparable<TopInstance> {
    DateTime getDateTime();
}
