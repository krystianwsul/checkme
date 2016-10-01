package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.Map;

class WeeklyScheduleEntry extends ScheduleEntry {
    @NonNull
    private final DayOfWeek mDayOfWeek;

    @NonNull
    private final TimePairPersist mTimePairPersist;

    WeeklyScheduleEntry(@NonNull CreateTaskLoader.WeeklyScheduleData weeklyScheduleData) {
        mDayOfWeek = weeklyScheduleData.DayOfWeek;
        mTimePairPersist = new TimePairPersist(weeklyScheduleData.TimePair);
    }

    private WeeklyScheduleEntry(@NonNull DayOfWeek dayOfWeek, @NonNull TimePairPersist timePairPersist, @Nullable String error) {
        super(error);

        mDayOfWeek = dayOfWeek;
        mTimePairPersist = timePairPersist;
    }

    WeeklyScheduleEntry(@NonNull ScheduleDialogFragment.ScheduleDialogData scheduleDialogData) {
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.WEEKLY);

        mDayOfWeek = scheduleDialogData.mDayOfWeek;
        mTimePairPersist = scheduleDialogData.mTimePairPersist;
    }

    @NonNull
    @Override
    String getText(@NonNull Map<Integer, CreateTaskLoader.CustomTimeData> customTimeDatas, @NonNull Context context) {
        if (mTimePairPersist.getCustomTimeId() != null) {
            CreateTaskLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePairPersist.getCustomTimeId());
            Assert.assertTrue(customTimeData != null);

            return mDayOfWeek + ", " + customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDayOfWeek) + ")";
        } else {
            return mDayOfWeek + ", " + mTimePairPersist.getHourMinute().toString();
        }
    }

    @NonNull
    @Override
    CreateTaskLoader.ScheduleData getScheduleData() {
        return new CreateTaskLoader.WeeklyScheduleData(mDayOfWeek, mTimePairPersist.getTimePair());
    }

    @NonNull
    @Override
    ScheduleDialogFragment.ScheduleDialogData getScheduleDialogData(@NonNull Date today, @Nullable CreateTaskActivity.ScheduleHint scheduleHint) {
        Date date = (scheduleHint != null ? scheduleHint.mDate : today);

        int monthDayNumber = date.getDay();
        boolean beginningOfMonth = true;
        if (monthDayNumber > 28) {
            monthDayNumber = getDaysInMonth(date) - monthDayNumber + 1;
            beginningOfMonth = false;
        }
        int monthWeekNumber = (monthDayNumber - 1) / 7 + 1;

        return new ScheduleDialogFragment.ScheduleDialogData(date, mDayOfWeek, true, monthDayNumber, monthWeekNumber, date.getDayOfWeek(), beginningOfMonth, mTimePairPersist, ScheduleType.WEEKLY);
    }

    @NonNull
    @Override
    ScheduleType getScheduleType() {
        return ScheduleType.WEEKLY;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeSerializable(mDayOfWeek);
        parcel.writeParcelable(mTimePairPersist, 0);
        parcel.writeString(mError);
    }

    @SuppressWarnings("unused")
    public static final Creator<WeeklyScheduleEntry> CREATOR = new Creator<WeeklyScheduleEntry>() {
        @Override
        public WeeklyScheduleEntry createFromParcel(Parcel in) {
            DayOfWeek dayOfWeek = (DayOfWeek) in.readSerializable();
            Assert.assertTrue(dayOfWeek != null);

            TimePairPersist timePairPersist = in.readParcelable(TimePairPersist.class.getClassLoader());
            Assert.assertTrue(timePairPersist != null);

            String error = in.readString();

            return new WeeklyScheduleEntry(dayOfWeek, timePairPersist, error);
        }

        @Override
        public WeeklyScheduleEntry[] newArray(int size) {
            return new WeeklyScheduleEntry[size];
        }
    };
}
