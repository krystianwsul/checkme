package com.example.krystianwsul.organizator.gui.instances;

import android.os.Bundle;

import com.example.krystianwsul.organizator.domainmodel.CustomTime;
import com.example.krystianwsul.organizator.domainmodel.Instance;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.NormalTime;
import com.example.krystianwsul.organizator.utils.time.Time;

import junit.framework.Assert;

public class NewInstanceData {
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
