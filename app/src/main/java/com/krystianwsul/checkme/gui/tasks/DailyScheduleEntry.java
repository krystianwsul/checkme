package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.Map;

class DailyScheduleEntry extends ScheduleEntry {
    @NonNull
    private final TimePairPersist mTimePairPersist;

    DailyScheduleEntry(@NonNull CreateTaskLoader.DailyScheduleData dailyScheduleData) {
        mTimePairPersist = new TimePairPersist(dailyScheduleData.TimePair);
    }

    private DailyScheduleEntry(@NonNull TimePairPersist timePairPersist, @Nullable String error) { // replace with more specific
        super(error);

        mTimePairPersist = timePairPersist;
    }

    DailyScheduleEntry(@NonNull ScheduleDialogFragment.ScheduleDialogData scheduleDialogData) {
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.DAILY);

        mTimePairPersist = scheduleDialogData.mTimePairPersist;
    }

    @NonNull
    @Override
    String getText(@NonNull Map<Integer, CreateTaskLoader.CustomTimeData> customTimeDatas, @NonNull Context context) {
        if (mTimePairPersist.getCustomTimeId() != null) {
            CreateTaskLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePairPersist.getCustomTimeId());
            Assert.assertTrue(customTimeData != null);

            return customTimeData.Name;
        } else {
            return mTimePairPersist.getHourMinute().toString();
        }
    }

    @NonNull
    @Override
    CreateTaskLoader.ScheduleData getScheduleData() {
        return new CreateTaskLoader.DailyScheduleData(mTimePairPersist.getTimePair());
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

        return new ScheduleDialogFragment.ScheduleDialogData(date, date.getDayOfWeek(), true, monthDayNumber, monthWeekNumber, date.getDayOfWeek(), beginningOfMonth, mTimePairPersist, ScheduleType.DAILY);
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
        parcel.writeParcelable(mTimePairPersist, 0);
        parcel.writeString(mError);
    }

    @SuppressWarnings("unused")
    public static final Creator<DailyScheduleEntry> CREATOR = new Creator<DailyScheduleEntry>() {
        @Override
        public DailyScheduleEntry createFromParcel(Parcel in) {
            TimePairPersist timePairPersist = in.readParcelable(TimePairPersist.class.getClassLoader());
            Assert.assertTrue(timePairPersist != null);

            String error = in.readString();

            return new DailyScheduleEntry(timePairPersist, error);
        }

        @Override
        public DailyScheduleEntry[] newArray(int size) {
            return new DailyScheduleEntry[size];
        }
    };
}
