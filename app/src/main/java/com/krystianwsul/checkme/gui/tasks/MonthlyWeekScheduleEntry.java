package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.Utils;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.Collections;
import java.util.Map;

class MonthlyWeekScheduleEntry extends ScheduleEntry {
    private final int mMonthWeekNumber;

    @NonNull
    private final DayOfWeek mMonthWeekDay;

    private final boolean mBeginningOfMonth;

    @NonNull
    private final TimePair mTimePair;

    MonthlyWeekScheduleEntry(@NonNull CreateTaskLoader.ScheduleData.MonthlyWeekScheduleData monthlyWeekScheduleData) {
        mMonthWeekNumber = monthlyWeekScheduleData.getDayOfMonth();
        mMonthWeekDay = monthlyWeekScheduleData.getDayOfWeek();
        mBeginningOfMonth = monthlyWeekScheduleData.getBeginningOfMonth();
        mTimePair = monthlyWeekScheduleData.getTimePair().copy();
    }

    private MonthlyWeekScheduleEntry(int monthWeekNumber, @NonNull DayOfWeek monthWeekDay, boolean beginningOfMonth, @NonNull TimePair timePair, @Nullable String error) {
        super(error);

        mMonthWeekNumber = monthWeekNumber;
        mMonthWeekDay = monthWeekDay;
        mBeginningOfMonth = beginningOfMonth;
        mTimePair = timePair;
    }

    MonthlyWeekScheduleEntry(@NonNull ScheduleDialogFragment.ScheduleDialogData scheduleDialogData) {
        Assert.assertTrue(scheduleDialogData.getMScheduleType() == ScheduleType.MONTHLY_WEEK);
        Assert.assertTrue(!scheduleDialogData.getMMonthlyDay());

        mMonthWeekNumber = scheduleDialogData.getMMonthWeekNumber();
        mMonthWeekDay = scheduleDialogData.getMMonthWeekDay();
        mBeginningOfMonth = scheduleDialogData.getMBeginningOfMonth();
        mTimePair = scheduleDialogData.getMTimePairPersist().getTimePair();
    }

    @NonNull
    @Override
    public String getText(@NonNull Map<CustomTimeKey, CreateTaskLoader.CustomTimeData> customTimeDatas, @NonNull Context context) {
        String day = Utils.ordinal(mMonthWeekNumber) + " " + mMonthWeekDay + " " + context.getString(R.string.monthDayStart) + " " + context.getResources().getStringArray(R.array.month)[mBeginningOfMonth ? 0 : 1] + " " + context.getString(R.string.monthDayEnd);

        if (mTimePair.mCustomTimeKey != null) {
            Assert.assertTrue(mTimePair.mHourMinute == null);

            CreateTaskLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePair.mCustomTimeKey);
            Assert.assertTrue(customTimeData != null);

            return day + ", " + customTimeData.getName();
        } else {
            Assert.assertTrue(mTimePair.mHourMinute != null);

            return day + ", " + mTimePair.mHourMinute.toString();
        }
    }

    @NonNull
    @Override
    public CreateTaskLoader.ScheduleData getScheduleData() {
        return new CreateTaskLoader.ScheduleData.MonthlyWeekScheduleData(mMonthWeekNumber, mMonthWeekDay, mBeginningOfMonth, mTimePair);
    }

    @NonNull
    @Override
    public ScheduleDialogFragment.ScheduleDialogData getScheduleDialogData(@NonNull Date today, @Nullable CreateTaskActivity.ScheduleHint scheduleHint) {
        Date date = (scheduleHint != null ? scheduleHint.getMDate() : today);

        date = Utils.getDateInMonth(date.getYear(), date.getMonth(), mMonthWeekNumber, mMonthWeekDay, mBeginningOfMonth);

        return new ScheduleDialogFragment.ScheduleDialogData(date, Collections.singleton(mMonthWeekDay), false, date.getDay(), mMonthWeekNumber, mMonthWeekDay, mBeginningOfMonth, new TimePairPersist(mTimePair), ScheduleType.MONTHLY_WEEK);
    }

    @NonNull
    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.MONTHLY_WEEK;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mMonthWeekNumber);
        parcel.writeSerializable(mMonthWeekDay);
        parcel.writeInt(mBeginningOfMonth ? 1 : 0);
        parcel.writeParcelable(mTimePair, 0);
        parcel.writeString(getError());
    }

    @SuppressWarnings("unused")
    public static final Creator<MonthlyWeekScheduleEntry> CREATOR = new Creator<MonthlyWeekScheduleEntry>() {
        @Override
        public MonthlyWeekScheduleEntry createFromParcel(Parcel in) {
            int monthWeekNumber = in.readInt();

            DayOfWeek monthWeekDay = (DayOfWeek) in.readSerializable();
            Assert.assertTrue(monthWeekDay != null);

            boolean beginningOfMonth = (in.readInt() == 1);

            TimePair timePair = in.readParcelable(TimePair.class.getClassLoader());
            Assert.assertTrue(timePair != null);

            String error = in.readString();

            return new MonthlyWeekScheduleEntry(monthWeekNumber, monthWeekDay, beginningOfMonth, timePair, error);
        }

        @Override
        public MonthlyWeekScheduleEntry[] newArray(int size) {
            return new MonthlyWeekScheduleEntry[size];
        }
    };
}
