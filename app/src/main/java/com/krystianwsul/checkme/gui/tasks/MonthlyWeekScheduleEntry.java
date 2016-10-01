package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.Map;

class MonthlyWeekScheduleEntry extends ScheduleEntry {
    private final int mMonthWeekNumber;

    @NonNull
    private final DayOfWeek mMonthWeekDay;

    private final boolean mBeginningOfMonth;

    @NonNull
    private final TimePairPersist mTimePairPersist;

    private MonthlyWeekScheduleEntry(int monthWeekNumber, @NonNull DayOfWeek monthWeekDay, boolean beginningOfMonth, @NonNull TimePairPersist timePairPersist, @Nullable String error) {
        super(error);

        mMonthWeekNumber = monthWeekNumber;
        mMonthWeekDay = monthWeekDay;
        mBeginningOfMonth = beginningOfMonth;
        mTimePairPersist = timePairPersist;
    }

    MonthlyWeekScheduleEntry(@NonNull ScheduleDialogFragment.ScheduleDialogData scheduleDialogData) {
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.MONTHLY_WEEK);
        Assert.assertTrue(!scheduleDialogData.mMonthlyDay);

        mMonthWeekNumber = scheduleDialogData.mMonthWeekNumber;
        mMonthWeekDay = scheduleDialogData.mMonthWeekDay;
        mBeginningOfMonth = scheduleDialogData.mBeginningOfMonth;
        mTimePairPersist = scheduleDialogData.mTimePairPersist;
    }

    @NonNull
    @Override
    String getText(@NonNull Map<Integer, CreateTaskLoader.CustomTimeData> customTimeDatas, @NonNull Context context) {
        String day = mMonthWeekNumber + " " + mMonthWeekDay + " " + context.getString(R.string.monthDayStart) + " " + context.getResources().getStringArray(R.array.month)[mBeginningOfMonth ? 0 : 1] + " " + context.getString(R.string.monthDayEnd);

        if (mTimePairPersist.getCustomTimeId() != null) {
            CreateTaskLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePairPersist.getCustomTimeId());
            Assert.assertTrue(customTimeData != null);

            return day + ", " + customTimeData.Name;
        } else {
            return day + ", " + mTimePairPersist.getHourMinute();
        }
    }

    @NonNull
    @Override
    CreateTaskLoader.ScheduleData getScheduleData() {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    ScheduleDialogFragment.ScheduleDialogData getScheduleDialogData(@NonNull Date today, @Nullable CreateTaskActivity.ScheduleHint scheduleHint) {
        Date date = (scheduleHint != null ? scheduleHint.mDate : today);

        if (mBeginningOfMonth) {
            Date first = new Date(date.getYear(), date.getMonth(), 1);
            int day = mMonthWeekNumber * 7 - (first.getDayOfWeek().ordinal() - mMonthWeekDay.ordinal());
            date = new Date(date.getYear(), date.getMonth(), day);
        } else {
            Date last = new Date(date.getYear(), date.getMonth(), 1);
            int day = mMonthWeekNumber * 7 + (last.getDayOfWeek().ordinal() - mMonthWeekDay.ordinal());
            date = new Date(date.getYear(), date.getMonth(), getDaysInMonth(date) - day + 1);
        }

        return new ScheduleDialogFragment.ScheduleDialogData(date, mMonthWeekDay, false, date.getDay(), mMonthWeekNumber, mMonthWeekDay, mBeginningOfMonth, mTimePairPersist, ScheduleType.MONTHLY_WEEK);
    }

    @NonNull
    @Override
    ScheduleType getScheduleType() {
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
        parcel.writeParcelable(mTimePairPersist, 0);
        parcel.writeString(mError);
    }

    @SuppressWarnings("unused")
    public static final Creator<MonthlyWeekScheduleEntry> CREATOR = new Creator<MonthlyWeekScheduleEntry>() {
        @Override
        public MonthlyWeekScheduleEntry createFromParcel(Parcel in) {
            int monthWeekNumber = in.readInt();

            DayOfWeek monthWeekDay = (DayOfWeek) in.readSerializable();
            Assert.assertTrue(monthWeekDay != null);

            boolean beginningOfMonth = (in.readInt() == 1);

            TimePairPersist timePairPersist = in.readParcelable(TimePairPersist.class.getClassLoader());
            Assert.assertTrue(timePairPersist != null);

            String error = in.readString();

            return new MonthlyWeekScheduleEntry(monthWeekNumber, monthWeekDay, beginningOfMonth, timePairPersist, error);
        }

        @Override
        public MonthlyWeekScheduleEntry[] newArray(int size) {
            return new MonthlyWeekScheduleEntry[size];
        }
    };
}
