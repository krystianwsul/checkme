package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.SingleInstance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.SingleScheduleRecord;
import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Krystian on 10/17/2015.
 */
public abstract class SingleSchedule extends Schedule {
    protected final SingleScheduleRecord mSingleScheduleRecord;

    private final static HashMap<Integer, SingleSchedule> sSingleSchedules = new HashMap<>();

    public static SingleSchedule getSingleSchedule(Task task) {
        Assert.assertTrue(task != null);
        if (sSingleSchedules.containsKey(task.getId())) {
            return sSingleSchedules.get(task.getId());
        } else {
            SingleSchedule singleSchedule = createSingleSchedule(task);
            if (singleSchedule == null)
                return null;

            sSingleSchedules.put(task.getId(), singleSchedule);
            return singleSchedule;
        }
    }

    private static SingleSchedule createSingleSchedule(Task task) {
        SingleScheduleRecord singleScheduleRecord = PersistenceManger.getInstance().getSingleScheduleRecord(task.getId());
        if (singleScheduleRecord == null)
            return null;

        if (singleScheduleRecord.getTimeRecordId() == null)
            return new SingleNormalSchedule(singleScheduleRecord, task);
        else
            return new SingleCustomSchedule(singleScheduleRecord, task);
    }

    protected SingleSchedule(SingleScheduleRecord singleScheduleRecord, Task task) {
        super(task);

        Assert.assertTrue(singleScheduleRecord != null);

        mSingleScheduleRecord = singleScheduleRecord;
    }

    private DateTime getDateTime() {
        return new DateTime(getDate(), getTime());
    }

    private Date getDate() {
        return new Date(mSingleScheduleRecord.getYear(), mSingleScheduleRecord.getMonth(), mSingleScheduleRecord.getDay());
    }

    protected abstract Time getTime();

    public ArrayList<Instance> getInstances(TimeStamp givenStartTimeStamp, TimeStamp givenEndTimeStamp) {
        Assert.assertTrue(givenEndTimeStamp != null);

        ArrayList<Instance> instances = new ArrayList<>();

        DateTime dateTime = getDateTime();

        TimeStamp timeStamp = new TimeStamp(dateTime.getDate(), dateTime.getTime().getTimeByDay(dateTime.getDate().getDayOfWeek()));

        if (givenStartTimeStamp != null && (givenStartTimeStamp.compareTo(timeStamp) > 0))
            return instances;

        if (givenEndTimeStamp.compareTo(timeStamp) < 0)
            return instances;

        instances.add(SingleInstance.getSingleInstance(mTask));

        return instances;
    }

    public String getTaskText(Context context) {
        return getDateTime().getDisplayText(context);
    }
}
