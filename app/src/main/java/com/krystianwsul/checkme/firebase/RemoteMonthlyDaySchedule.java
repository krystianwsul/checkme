package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.firebase.json.MonthlyDayScheduleJson;
import com.krystianwsul.checkme.firebase.json.ScheduleJson;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;

import junit.framework.Assert;

public class RemoteMonthlyDaySchedule extends RemoteSchedule {
    private final int mPosition;

    @NonNull
    private MonthlyDayScheduleJson mMonthlyDayScheduleJson;

    public RemoteMonthlyDaySchedule(int position, @NonNull MonthlyDayScheduleJson monthlyDayScheduleJson) {
        mPosition = position;
        mMonthlyDayScheduleJson = monthlyDayScheduleJson;
    }

    @NonNull
    @Override
    protected ScheduleJson getRemoteScheduleRecord() {
        return mMonthlyDayScheduleJson;
    }

    @NonNull
    @Override
    String getScheduleText(@NonNull Context context) {
        String day = mMonthlyDayScheduleJson.getDayOfMonth() + " " + context.getString(R.string.monthDay) + " " + context.getString(R.string.monthDayStart) + " " + context.getResources().getStringArray(R.array.month)[mMonthlyDayScheduleJson.getBeginningOfMonth() ? 0 : 1] + " " + context.getString(R.string.monthDayEnd);

        return day + ": " + getTime();
    }

    @NonNull
    Time getTime() {
        Assert.assertTrue(mMonthlyDayScheduleJson.getCustomTimeId() == null); // todo customtime
        Assert.assertTrue(mMonthlyDayScheduleJson.getHour() != null);
        Assert.assertTrue(mMonthlyDayScheduleJson.getMinute() != null);

        //Integer customTimeId = mMonthlyDayScheduleRecord.getCustomTimeId();
        //if (customTimeId != null) {
        //    return mDomainFactory.getCustomTime(mMonthlyDayScheduleRecord.getCustomTimeId());
        //} else {
        Integer hour = mMonthlyDayScheduleJson.getHour();
        Integer minute = mMonthlyDayScheduleJson.getMinute();
        Assert.assertTrue(hour != null);
        Assert.assertTrue(minute != null);
        return new NormalTime(hour, minute);
        //}
    }

    @NonNull
    @Override
    public String getPath() {
        return "monthlyDayScheduleRecords/" + mPosition;
    }
}
