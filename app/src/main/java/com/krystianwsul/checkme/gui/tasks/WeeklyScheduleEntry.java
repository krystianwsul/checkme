package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.Utils;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.Map;

class WeeklyScheduleEntry extends ScheduleEntry {
    @NonNull
    private final DayOfWeek mDayOfWeek;

    @NonNull
    private final TimePair mTimePair;

    WeeklyScheduleEntry(@NonNull CreateTaskLoader.WeeklyScheduleData weeklyScheduleData) {
        mDayOfWeek = weeklyScheduleData.DayOfWeek;
        mTimePair = weeklyScheduleData.TimePair.copy();
    }

    private WeeklyScheduleEntry(@NonNull DayOfWeek dayOfWeek, @NonNull TimePair timePair, @Nullable String error) {
        super(error);

        mDayOfWeek = dayOfWeek;
        mTimePair = timePair;
    }

    WeeklyScheduleEntry(@NonNull ScheduleDialogFragment.ScheduleDialogData scheduleDialogData) {
        Assert.assertTrue(scheduleDialogData.getMScheduleType() == ScheduleType.WEEKLY);

        mDayOfWeek = scheduleDialogData.getMDayOfWeek();
        mTimePair = scheduleDialogData.getMTimePairPersist().getTimePair();
    }

    @NonNull
    @Override
    String getText(@NonNull Map<CustomTimeKey, CreateTaskLoader.CustomTimeData> customTimeDatas, @NonNull Context context) {
        if (mTimePair.mCustomTimeKey != null) {
            Assert.assertTrue(mTimePair.mHourMinute == null);

            CreateTaskLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePair.mCustomTimeKey);
            Assert.assertTrue(customTimeData != null);

            return mDayOfWeek.toString() + ", " + customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDayOfWeek).toString() + ")";
        } else {
            Assert.assertTrue(mTimePair.mHourMinute != null);

            return mDayOfWeek.toString() + ", " + mTimePair.mHourMinute.toString();
        }
    }

    @NonNull
    @Override
    CreateTaskLoader.ScheduleData getScheduleData() {
        return new CreateTaskLoader.WeeklyScheduleData(mDayOfWeek, mTimePair);
    }

    @NonNull
    @Override
    ScheduleDialogFragment.ScheduleDialogData getScheduleDialogData(@NonNull Date today, @Nullable CreateTaskActivity.ScheduleHint scheduleHint) {
        Date date = (scheduleHint != null ? scheduleHint.mDate : today);

        int monthDayNumber = date.getDay();
        boolean beginningOfMonth = true;
        if (monthDayNumber > 28) {
            monthDayNumber = Utils.getDaysInMonth(date.getYear(), date.getMonth()) - monthDayNumber + 1;
            beginningOfMonth = false;
        }
        int monthWeekNumber = (monthDayNumber - 1) / 7 + 1;

        return new ScheduleDialogFragment.ScheduleDialogData(date, mDayOfWeek, true, monthDayNumber, monthWeekNumber, date.getDayOfWeek(), beginningOfMonth, new TimePairPersist(mTimePair), ScheduleType.WEEKLY);
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
        parcel.writeParcelable(mTimePair, 0);
        parcel.writeString(mError);
    }

    @SuppressWarnings("unused")
    public static final Creator<WeeklyScheduleEntry> CREATOR = new Creator<WeeklyScheduleEntry>() {
        @Override
        public WeeklyScheduleEntry createFromParcel(Parcel in) {
            DayOfWeek dayOfWeek = (DayOfWeek) in.readSerializable();
            Assert.assertTrue(dayOfWeek != null);

            TimePair timePair = in.readParcelable(TimePair.class.getClassLoader());
            Assert.assertTrue(timePair != null);

            String error = in.readString();

            return new WeeklyScheduleEntry(dayOfWeek, timePair, error);
        }

        @Override
        public WeeklyScheduleEntry[] newArray(int size) {
            return new WeeklyScheduleEntry[size];
        }
    };
}
