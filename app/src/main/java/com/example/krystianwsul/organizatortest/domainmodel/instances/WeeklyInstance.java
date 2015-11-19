package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.WeeklyRepetition;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.ChildTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.persistencemodel.InstanceRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 11/9/2015.
 */
public class WeeklyInstance extends Instance {
    private final WeeklyRepetition mWeeklyRepetition;

    WeeklyInstance(Task task, WeeklyRepetition weeklyRepetition, InstanceRecord instanceRecord) {
        super(task, instanceRecord);

        Assert.assertTrue(weeklyRepetition != null);
        mWeeklyRepetition = weeklyRepetition;
    }

    WeeklyInstance(Task task, WeeklyRepetition weeklyRepetition, int id) {
        super(task, id);

        Assert.assertTrue(weeklyRepetition != null);
        mWeeklyRepetition = weeklyRepetition;
    }

    public int getWeeklyRepetitionId() {
        return mWeeklyRepetition.getId();
    }

    public String getScheduleText(Context context) {
        Assert.assertTrue(context != null);

        if (mTask.getParentTask() == null)
            return mWeeklyRepetition.getRepetitionDateTime().getDisplayText(context);
        else
            return null;
    }

    public Instance getChildInstance(ChildTask childTask) {
        Assert.assertTrue(childTask != null);
        return InstanceFactory.getInstance().getWeeklyInstance(childTask, mWeeklyRepetition);
    }

    public DateTime getDateTime() {
        return mWeeklyRepetition.getRepetitionDateTime();
    }

    protected InstanceRecord createInstanceRecord() {
        return PersistenceManger.getInstance().createWeeklyInstanceRecord(mId, mTask.getId(), mInstanceRecord.getId(), TimeStamp.getNow().getLong());
    }
}
