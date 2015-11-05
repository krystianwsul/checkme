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

    public static SingleSchedule getSingleSchedule(int taskId) {
        if (sSingleSchedules.containsKey(taskId)) {
            return sSingleSchedules.get(taskId);
        } else {
            SingleSchedule singleSchedule = createSingleSchedule(taskId);
            if (singleSchedule == null)
                return null;

            sSingleSchedules.put(taskId, singleSchedule);
            return singleSchedule;
        }
    }

    private static SingleSchedule createSingleSchedule(int taskId) {
        SingleScheduleRecord singleScheduleRecord = PersistenceManger.getInstance().getSingleScheduleRecord(taskId);
        if (singleScheduleRecord == null)
            return null;

        if (singleScheduleRecord.getTimeRecordId() == null)
            return new SingleNormalSchedule(taskId);
        else
            return new SingleCustomSchedule(taskId);
    }

    protected SingleSchedule(int taskId) {
        mSingleScheduleRecord = PersistenceManger.getInstance().getSingleScheduleRecord(taskId);
        Assert.assertTrue(mSingleScheduleRecord != null);
    }

    private DateTime getDateTime() {
        return new DateTime(getDate(), getTime());
    }

    private Date getDate() {
        return new Date(mSingleScheduleRecord.getYear(), mSingleScheduleRecord.getMonth(), mSingleScheduleRecord.getDay());
    }

    protected abstract Time getTime();

    public ArrayList<Instance> getInstances(Task task, TimeStamp givenStartTimeStamp, TimeStamp givenEndTimeStamp) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(givenEndTimeStamp != null);

        ArrayList<Instance> instances = new ArrayList<>();

        DateTime dateTime = getDateTime();

        TimeStamp timeStamp = new TimeStamp(dateTime.getDate(), dateTime.getTime().getTimeByDay(dateTime.getDate().getDayOfWeek()));

        if (givenStartTimeStamp != null && (givenStartTimeStamp.compareTo(timeStamp) > 0))
            return instances;

        if (givenEndTimeStamp.compareTo(timeStamp) < 0)
            return instances;

        instances.add(SingleInstance.getSingleInstance(task));

        return instances;
    }

    public String getTaskText(Context context) {
        return getDateTime().getDisplayText(context);
    }

    public int getTaskId() {
        return mSingleScheduleRecord.getTaskId();
    }
}
