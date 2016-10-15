package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.firebase.json.MonthlyWeekScheduleJson;
import com.krystianwsul.checkme.firebase.json.ScheduleJson;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;

import junit.framework.Assert;

public class RemoteMonthlyWeekSchedule extends RemoteSchedule {
    private final int mPosition;

    @NonNull
    MonthlyWeekScheduleJson mMonthlyWeekScheduleJson;

    public RemoteMonthlyWeekSchedule(int position, @NonNull MonthlyWeekScheduleJson monthlyWeekScheduleJson) {
        mPosition = position;
        mMonthlyWeekScheduleJson = monthlyWeekScheduleJson;
    }

    @NonNull
    @Override
    protected ScheduleJson getRemoteScheduleRecord() {
        return mMonthlyWeekScheduleJson;
    }

    @NonNull
    @Override
    String getScheduleText(@NonNull Context context) {
        String day = mMonthlyWeekScheduleJson.getDayOfMonth() + " " + getDayOfWeek() + " " + context.getString(R.string.monthDayStart) + " " + context.getResources().getStringArray(R.array.month)[mMonthlyWeekScheduleJson.getBeginningOfMonth() ? 0 : 1] + " " + context.getString(R.string.monthDayEnd);

        return day + ": " + getTime();
    }

    @NonNull
    DayOfWeek getDayOfWeek() {
        DayOfWeek dayOfWeek = DayOfWeek.values()[mMonthlyWeekScheduleJson.getDayOfWeek()];
        Assert.assertTrue(dayOfWeek != null);

        return dayOfWeek;
    }

    @NonNull
    Time getTime() {
        Assert.assertTrue(mMonthlyWeekScheduleJson.getCustomTimeId() == null); // todo customtime
        Assert.assertTrue(mMonthlyWeekScheduleJson.getHour() != null);
        Assert.assertTrue(mMonthlyWeekScheduleJson.getMinute() != null);

        //Integer customTimeId = mMonthlyDayScheduleRecord.getCustomTimeId();
        //if (customTimeId != null) {
        //    return mDomainFactory.getCustomTime(mMonthlyDayScheduleRecord.getCustomTimeId());
        //} else {
        Integer hour = mMonthlyWeekScheduleJson.getHour();
        Integer minute = mMonthlyWeekScheduleJson.getMinute();
        Assert.assertTrue(hour != null);
        Assert.assertTrue(minute != null);
        return new NormalTime(hour, minute);
        //}
    }

    @NonNull
    @Override
    public String getPath() {
        return "monthlyWeekScheduleRecords/" + mPosition;
    }
}
