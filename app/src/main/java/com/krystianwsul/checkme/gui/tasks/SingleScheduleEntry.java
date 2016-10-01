package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.Utils;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.Map;

class SingleScheduleEntry extends ScheduleEntry {
    @NonNull
    final Date mDate;

    @NonNull
    final TimePairPersist mTimePairPersist;

    SingleScheduleEntry(@NonNull CreateTaskLoader.SingleScheduleData singleScheduleData) {
        mDate = singleScheduleData.Date;
        mTimePairPersist = new TimePairPersist(singleScheduleData.TimePair);
    }

    SingleScheduleEntry(@Nullable CreateTaskActivity.ScheduleHint scheduleHint) {
        if (scheduleHint == null) { // new for task
            mDate = Date.today();
            mTimePairPersist = new TimePairPersist();
        } else if (scheduleHint.mTimePair != null) { // for instance group or instance join
            mDate = scheduleHint.mDate;
            mTimePairPersist = new TimePairPersist(scheduleHint.mTimePair);
        } else { // for group root
            mDate = scheduleHint.mDate;
            mTimePairPersist = new TimePairPersist();
        }
    }

    private SingleScheduleEntry(@NonNull Date date, @NonNull TimePairPersist timePairPersist, @Nullable String error) {
        super(error);

        mDate = date;
        mTimePairPersist = timePairPersist;
    }

    SingleScheduleEntry(@NonNull ScheduleDialogFragment.ScheduleDialogData scheduleDialogData) {
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.SINGLE);

        mDate = scheduleDialogData.mDate;
        mTimePairPersist = scheduleDialogData.mTimePairPersist;
    }

    @NonNull
    @Override
    String getText(@NonNull Map<Integer, CreateTaskLoader.CustomTimeData> customTimeDatas, @NonNull Context context) {
        if (mTimePairPersist.getCustomTimeId() != null) {
            CreateTaskLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePairPersist.getCustomTimeId());
            Assert.assertTrue(customTimeData != null);

            return mDate.getDisplayText(context) + ", " + customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDate.getDayOfWeek()) + ")";
        } else {
            return mDate.getDisplayText(context) + ", " + mTimePairPersist.getHourMinute().toString();
        }
    }

    @NonNull
    @Override
    CreateTaskLoader.ScheduleData getScheduleData() {
        return new CreateTaskLoader.SingleScheduleData(mDate, mTimePairPersist.getTimePair());
    }

    @NonNull
    @Override
    ScheduleDialogFragment.ScheduleDialogData getScheduleDialogData(@NonNull Date today, @Nullable CreateTaskActivity.ScheduleHint scheduleHint) {
        int monthDayNumber = mDate.getDay();
        boolean beginningOfMonth = true;
        if (monthDayNumber > 28) {
            monthDayNumber = Utils.getDaysInMonth(mDate.getYear(), mDate.getMonth()) - monthDayNumber + 1;
            beginningOfMonth = false;
        }
        int monthWeekNumber = (monthDayNumber - 1) / 7 + 1;

        return new ScheduleDialogFragment.ScheduleDialogData(mDate, mDate.getDayOfWeek(), true, monthDayNumber, monthWeekNumber, mDate.getDayOfWeek(), beginningOfMonth, mTimePairPersist, ScheduleType.SINGLE);
    }

    @NonNull
    @Override
    ScheduleType getScheduleType() {
        return ScheduleType.SINGLE;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(mDate, 0);
        parcel.writeParcelable(mTimePairPersist, 0);
        parcel.writeString(mError);
    }

    @SuppressWarnings("unused")
    public static final Creator<SingleScheduleEntry> CREATOR = new Creator<SingleScheduleEntry>() {
        @Override
        public SingleScheduleEntry createFromParcel(Parcel in) {
            Date date = in.readParcelable(Date.class.getClassLoader());
            Assert.assertTrue(date != null);

            TimePairPersist timePairPersist = in.readParcelable(TimePairPersist.class.getClassLoader());
            Assert.assertTrue(timePairPersist != null);

            String error = in.readString();

            return new SingleScheduleEntry(date, timePairPersist, error);
        }

        @Override
        public SingleScheduleEntry[] newArray(int size) {
            return new SingleScheduleEntry[size];
        }
    };
}
