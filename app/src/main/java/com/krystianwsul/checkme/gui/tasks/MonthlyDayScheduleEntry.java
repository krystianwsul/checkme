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
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.Collections;
import java.util.Map;

class MonthlyDayScheduleEntry extends ScheduleEntry {
    private final int mMonthDayNumber;
    private final boolean mBeginningOfMonth;

    @NonNull
    private final TimePair mTimePair;

    MonthlyDayScheduleEntry(@NonNull CreateTaskLoader.ScheduleData.MonthlyDayScheduleData monthlyDayScheduleData) {
        mMonthDayNumber = monthlyDayScheduleData.getDayOfMonth();
        mBeginningOfMonth = monthlyDayScheduleData.getBeginningOfMonth();
        mTimePair = monthlyDayScheduleData.getTimePair().copy();
    }

    private MonthlyDayScheduleEntry(int monthDayNumber, boolean beginningOfMonth, @NonNull TimePair timePair, @Nullable String error) {
        super(error);

        mMonthDayNumber = monthDayNumber;
        mBeginningOfMonth = beginningOfMonth;
        mTimePair = timePair;
    }

    MonthlyDayScheduleEntry(@NonNull ScheduleDialogFragment.ScheduleDialogData scheduleDialogData) {
        Assert.assertTrue(scheduleDialogData.getMScheduleType() == ScheduleType.MONTHLY_DAY);
        Assert.assertTrue(scheduleDialogData.getMMonthlyDay());

        mMonthDayNumber = scheduleDialogData.getMMonthDayNumber();
        mBeginningOfMonth = scheduleDialogData.getMBeginningOfMonth();
        mTimePair = scheduleDialogData.getMTimePairPersist().getTimePair();
    }

    @NonNull
    @Override
    public String getText(@NonNull Map<CustomTimeKey, CreateTaskLoader.CustomTimeData> customTimeDatas, @NonNull Context context) {
        String day = Utils.ordinal(mMonthDayNumber) + " " + context.getString(R.string.monthDay) + " " + context.getString(R.string.monthDayStart) + " " + context.getResources().getStringArray(R.array.month)[mBeginningOfMonth ? 0 : 1] + " " + context.getString(R.string.monthDayEnd);

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
        return new CreateTaskLoader.ScheduleData.MonthlyDayScheduleData(mMonthDayNumber, mBeginningOfMonth, mTimePair);
    }

    @NonNull
    @Override
    public ScheduleDialogFragment.ScheduleDialogData getScheduleDialogData(@NonNull Date today, @Nullable CreateTaskActivity.ScheduleHint scheduleHint) {
        Date date = (scheduleHint != null ? scheduleHint.getMDate() : today);

        date = Utils.getDateInMonth(date.getYear(), date.getMonth(), mMonthDayNumber, mBeginningOfMonth);

        return new ScheduleDialogFragment.ScheduleDialogData(date, Collections.singleton(date.getDayOfWeek()), true, mMonthDayNumber, (mMonthDayNumber - 1) / 7 + 1, date.getDayOfWeek(), mBeginningOfMonth, new TimePairPersist(mTimePair), ScheduleType.MONTHLY_DAY);
    }

    @NonNull
    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.MONTHLY_DAY;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mMonthDayNumber);
        parcel.writeInt(mBeginningOfMonth ? 1 : 0);
        parcel.writeParcelable(mTimePair, 0);
        parcel.writeString(getError());
    }

    @SuppressWarnings("unused")
    public static final Creator<MonthlyDayScheduleEntry> CREATOR = new Creator<MonthlyDayScheduleEntry>() {
        @Override
        public MonthlyDayScheduleEntry createFromParcel(Parcel in) {
            int monthDayNumber = in.readInt();

            boolean beginningOfMonth = (in.readInt() == 1);

            TimePair timePair = in.readParcelable(TimePair.class.getClassLoader());
            Assert.assertTrue(timePair != null);

            String error = in.readString();

            return new MonthlyDayScheduleEntry(monthDayNumber, beginningOfMonth, timePair, error);
        }

        @Override
        public MonthlyDayScheduleEntry[] newArray(int size) {
            return new MonthlyDayScheduleEntry[size];
        }
    };
}
