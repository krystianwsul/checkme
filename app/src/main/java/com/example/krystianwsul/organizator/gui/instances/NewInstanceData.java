package com.example.krystianwsul.organizator.gui.instances;

import android.os.Bundle;

import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.HourMinute;

import junit.framework.Assert;

public class NewInstanceData {
    private static final String TASK_ID_KEY = "taskId";
    private static final String DATE_KEY = "date";
    private static final String CUSTOM_TIME_ID_KEY = "customTimeId";
    private static final String HOUR_MINUTE_KEY = "hourMinute";

    public static Bundle getBundle(int taskId, Date scheduleDate, Integer scheduleCustomTimeId, HourMinute scheduleHourMinute) {
        Assert.assertTrue(scheduleDate != null);
        Assert.assertTrue((scheduleCustomTimeId == null) != (scheduleHourMinute == null));

        Bundle bundle = new Bundle();
        bundle.putInt(TASK_ID_KEY, taskId);
        bundle.putParcelable(DATE_KEY, scheduleDate);

        if (scheduleCustomTimeId != null)
            bundle.putInt(CUSTOM_TIME_ID_KEY, scheduleCustomTimeId);
        else
            bundle.putParcelable(HOUR_MINUTE_KEY, scheduleHourMinute);

        return bundle;
    }

    public static int getTaskId(Bundle bundle) {
        Assert.assertTrue(bundle != null);

        int taskId = bundle.getInt(TASK_ID_KEY, -1);
        Assert.assertTrue(taskId != -1);

        return taskId;
    }

    public static Date getDate(Bundle bundle) {
        Assert.assertTrue(bundle != null);

        Date date = bundle.getParcelable(DATE_KEY);
        Assert.assertTrue(date != null);

        return date;
    }

    public static Integer getCustomTimeId(Bundle bundle) {
        Assert.assertTrue(bundle != null);

        int customTimeId = bundle.getInt(CUSTOM_TIME_ID_KEY, -1);

        return (customTimeId == -1 ? null : customTimeId);
    }

    public static HourMinute getHourMinute(Bundle bundle) {
        Assert.assertTrue(bundle != null);
        return bundle.getParcelable(HOUR_MINUTE_KEY);
    }
}
