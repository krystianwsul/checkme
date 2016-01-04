package com.example.krystianwsul.organizator.gui.instances;

import android.os.Bundle;

import com.example.krystianwsul.organizator.domainmodel.dates.Date;
import com.example.krystianwsul.organizator.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizator.domainmodel.instances.Instance;
import com.example.krystianwsul.organizator.domainmodel.instances.InstanceFactory;
import com.example.krystianwsul.organizator.domainmodel.tasks.Task;
import com.example.krystianwsul.organizator.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizator.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizator.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizator.domainmodel.times.HourMinute;
import com.example.krystianwsul.organizator.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizator.domainmodel.times.Time;

import junit.framework.Assert;

public class InstanceData {
    private static final String TASK_ID_KEY = "taskId";
    private static final String DATE_KEY = "date";
    private static final String CUSTOM_TIME_ID_KEY = "customTimeId";
    private static final String HOUR_MINUTE_KEY = "hourMinute";

    public static Bundle getBundle(Instance instance) {
        Assert.assertTrue(instance != null);

        Bundle bundle = new Bundle();
        bundle.putInt(TASK_ID_KEY, instance.getTaskId());
        bundle.putParcelable(DATE_KEY, instance.getScheduleDate());

        Time time = instance.getScheduleTime();
        if (time instanceof CustomTime) {
            bundle.putInt(CUSTOM_TIME_ID_KEY, ((CustomTime) time).getId());
        } else {
            Assert.assertTrue(time instanceof NormalTime);
            bundle.putParcelable(HOUR_MINUTE_KEY, ((NormalTime) time).getHourMinute());
        }

        return bundle;
    }

    public static Instance getInstance(Bundle bundle) {
        Assert.assertTrue(bundle != null);

        int taskId = bundle.getInt(TASK_ID_KEY, -1);
        Assert.assertTrue(taskId != -1);
        Task task = TaskFactory.getInstance().getTask(taskId);
        Assert.assertTrue(task != null);

        Date date = bundle.getParcelable(DATE_KEY);
        Assert.assertTrue(date != null);

        int customTimeId = bundle.getInt(CUSTOM_TIME_ID_KEY, -1);
        Time time;
        if (customTimeId != -1) {
            time = CustomTimeFactory.getInstance().getCustomTime(customTimeId);
            Assert.assertTrue(time != null);
        } else {
            HourMinute hourMinute = bundle.getParcelable(HOUR_MINUTE_KEY);
            Assert.assertTrue(hourMinute != null);

            time = new NormalTime(hourMinute);
        }

        DateTime dateTime = new DateTime(date, time);

        return InstanceFactory.getInstance().getInstance(task, dateTime);
    }
}
