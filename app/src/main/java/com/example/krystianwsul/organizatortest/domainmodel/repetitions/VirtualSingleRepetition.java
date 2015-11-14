package com.example.krystianwsul.organizatortest.domainmodel.repetitions;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.instances.VirtualSingleInstance;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.SingleSchedule;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

/**
 * Created by Krystian on 11/14/2015.
 */
public class VirtualSingleRepetition extends SingleRepetition {
    public VirtualSingleRepetition(SingleSchedule singleSchedule) {
        super(singleSchedule);
    }

    public Date getRepetitionDate() {
        return getScheduleDate();
    }

    public Time getRepetitionTime() {
        return getScheduleTime();
    }
}
