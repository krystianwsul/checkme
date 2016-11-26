package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.Utils;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.Map;

class SingleScheduleEntry extends ScheduleEntry {
    @NonNull
    final Date mDate;

    @NonNull
    final TimePair mTimePair;

    SingleScheduleEntry(@NonNull CreateTaskLoader.SingleScheduleData singleScheduleData) {
        mDate = singleScheduleData.Date;
        mTimePair = singleScheduleData.TimePair.copy();
    }

    SingleScheduleEntry(@Nullable CreateTaskActivity.ScheduleHint scheduleHint) {
        if (scheduleHint == null) { // new for task
            Pair<Date, HourMinute> pair = HourMinute.getNextHour();

            mDate = pair.first;
            mTimePair = new TimePair(pair.second);
        } else if (scheduleHint.mTimePair != null) { // for instance group or instance join
            mDate = scheduleHint.mDate;
            mTimePair = scheduleHint.mTimePair.copy();
        } else { // for group root
            Pair<Date, HourMinute> pair = HourMinute.getNextHour(scheduleHint.mDate);

            mDate = pair.first;
            mTimePair = new TimePair(pair.second);
        }
    }

    private SingleScheduleEntry(@NonNull Date date, @NonNull TimePair timePair, @Nullable String error) {
        super(error);

        mDate = date;
        mTimePair = timePair;
    }

    SingleScheduleEntry(@NonNull ScheduleDialogFragment.ScheduleDialogData scheduleDialogData) {
        Assert.assertTrue(scheduleDialogData.mScheduleType == ScheduleType.SINGLE);

        mDate = scheduleDialogData.mDate;
        mTimePair = scheduleDialogData.mTimePairPersist.getTimePair();
    }

    @NonNull
    @Override
    String getText(@NonNull Map<CustomTimeKey, CreateTaskLoader.CustomTimeData> customTimeDatas, @NonNull Context context) {
        if (mTimePair.mCustomTimeKey != null) {
            Assert.assertTrue(mTimePair.mHourMinute == null);

            CreateTaskLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePair.mCustomTimeKey);
            Assert.assertTrue(customTimeData != null);

            return mDate.getDisplayText(context) + ", " + customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDate.getDayOfWeek()) + ")";
        } else {
            Assert.assertTrue(mTimePair.mHourMinute != null);

            return mDate.getDisplayText(context) + ", " + mTimePair.mHourMinute.toString();
        }
    }

    @NonNull
    @Override
    CreateTaskLoader.ScheduleData getScheduleData() {
        return new CreateTaskLoader.SingleScheduleData(mDate, mTimePair);
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

        return new ScheduleDialogFragment.ScheduleDialogData(mDate, mDate.getDayOfWeek(), true, monthDayNumber, monthWeekNumber, mDate.getDayOfWeek(), beginningOfMonth, new TimePairPersist(mTimePair), ScheduleType.SINGLE);
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
        parcel.writeParcelable(mTimePair, 0);
        parcel.writeString(mError);
    }

    @SuppressWarnings("unused")
    public static final Creator<SingleScheduleEntry> CREATOR = new Creator<SingleScheduleEntry>() {
        @Override
        public SingleScheduleEntry createFromParcel(Parcel in) {
            Date date = in.readParcelable(Date.class.getClassLoader());
            Assert.assertTrue(date != null);

            TimePair timePair = in.readParcelable(TimePair.class.getClassLoader());
            Assert.assertTrue(timePair != null);

            String error = in.readString();

            return new SingleScheduleEntry(date, timePair, error);
        }

        @Override
        public SingleScheduleEntry[] newArray(int size) {
            return new SingleScheduleEntry[size];
        }
    };
}
