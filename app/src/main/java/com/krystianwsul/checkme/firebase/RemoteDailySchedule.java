package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.firebase.json.DailyScheduleJson;
import com.krystianwsul.checkme.firebase.json.ScheduleJson;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;

import junit.framework.Assert;

public class RemoteDailySchedule extends RemoteSchedule {
    private final int mPosition;

    @NonNull
    private DailyScheduleJson mDailyScheduleJson;

    public RemoteDailySchedule(int position, @NonNull DailyScheduleJson dailyScheduleJson) {
        mPosition = position;
        mDailyScheduleJson = dailyScheduleJson;
    }

    @NonNull
    @Override
    protected ScheduleJson getRemoteScheduleRecord() {
        return mDailyScheduleJson;
    }

    @NonNull
    @Override
    String getScheduleText(@NonNull Context context) {
        return context.getString(R.string.daily) + " " + getTime().toString();
    }

    @NonNull
    public Time getTime() {
        Assert.assertTrue(mDailyScheduleJson.getCustomTimeId() == null); // todo customtime
        Assert.assertTrue(mDailyScheduleJson.getHour() != null);
        Assert.assertTrue(mDailyScheduleJson.getMinute() != null);

        //Integer customTimeId = mDailyScheduleRecord.getCustomTimeId();
        //if (customTimeId != null) {
        //    return mDomainFactory.getCustomTime(mDailyScheduleRecord.getCustomTimeId());
        //} else {
        Integer hour = mDailyScheduleJson.getHour();
        Integer minute = mDailyScheduleJson.getMinute();
        Assert.assertTrue(hour != null);
        Assert.assertTrue(minute != null);
        return new NormalTime(hour, minute);
        //}
    }

    @NonNull
    @Override
    public String getPath() {
        return "dailyScheduleRecords/" + mPosition;
    }
}
