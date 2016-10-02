package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.Utils;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.Map;

class DailyScheduleEntry extends ScheduleEntry {
    @NonNull
    private final TimePair mTimePair;

    DailyScheduleEntry(@NonNull CreateTaskLoader.DailyScheduleData dailyScheduleData) {
        mTimePair = dailyScheduleData.TimePair.copy();
    }

    private DailyScheduleEntry(@NonNull TimePair timePair, @Nullable String error) {
        super(error);

        mTimePair = timePair;
    }

    DailyScheduleEntry(@NonNull ScheduleDialogFragment.ScheduleDialogData scheduleDialogData) {
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.DAILY);

        mTimePair = scheduleDialogData.mTimePairPersist.getTimePair();
    }

    @NonNull
    @Override
    String getText(@NonNull Map<Integer, CreateTaskLoader.CustomTimeData> customTimeDatas, @NonNull Context context) {
        if (mTimePair.mCustomTimeId != null) {
            Assert.assertTrue(mTimePair.mHourMinute == null);

            CreateTaskLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePair.mCustomTimeId);
            Assert.assertTrue(customTimeData != null);

            return customTimeData.Name;
        } else {
            Assert.assertTrue(mTimePair.mHourMinute != null);

            return mTimePair.mHourMinute.toString();
        }
    }

    @NonNull
    @Override
    CreateTaskLoader.ScheduleData getScheduleData() {
        return new CreateTaskLoader.DailyScheduleData(mTimePair);
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

        return new ScheduleDialogFragment.ScheduleDialogData(date, date.getDayOfWeek(), true, monthDayNumber, monthWeekNumber, date.getDayOfWeek(), beginningOfMonth, new TimePairPersist(mTimePair), ScheduleType.DAILY);
    }

    @NonNull
    @Override
    ScheduleType getScheduleType() {
        return ScheduleType.DAILY;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(mTimePair, 0);
        parcel.writeString(mError);
    }

    @SuppressWarnings("unused")
    public static final Creator<DailyScheduleEntry> CREATOR = new Creator<DailyScheduleEntry>() {
        @Override
        public DailyScheduleEntry createFromParcel(Parcel in) {
            TimePair timePair = in.readParcelable(TimePair.class.getClassLoader());
            Assert.assertTrue(timePair != null);

            String error = in.readString();

            return new DailyScheduleEntry(timePair, error);
        }

        @Override
        public DailyScheduleEntry[] newArray(int size) {
            return new DailyScheduleEntry[size];
        }
    };
}
