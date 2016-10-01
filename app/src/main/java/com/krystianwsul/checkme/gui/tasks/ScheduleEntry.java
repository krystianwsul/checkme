package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

class ScheduleEntry implements Parcelable {
    @NonNull
    final Date mDate;

    private final DayOfWeek mDayOfWeek;
    private boolean mMonthlyDay;
    private int mMonthDayNumber;
    private int mMonthWeekNumber;
    private DayOfWeek mMonthWeekDay;
    private boolean mBeginningOfMonth;

    @NonNull
    final TimePairPersist mTimePairPersist;
    final ScheduleType mScheduleType;

    @Nullable
    String mError;

    ScheduleEntry(@NonNull CreateTaskLoader.SingleScheduleData singleScheduleData) {
        mDate = singleScheduleData.Date;
        mDayOfWeek = mDate.getDayOfWeek();
        mTimePairPersist = new TimePairPersist(singleScheduleData.TimePair);
        mScheduleType = ScheduleType.SINGLE;
    }

    ScheduleEntry(@NonNull CreateTaskLoader.DailyScheduleData dailyScheduleData) {
        mDate = Date.today();
        mDayOfWeek = mDate.getDayOfWeek();
        mTimePairPersist = new TimePairPersist(dailyScheduleData.TimePair);
        mScheduleType = ScheduleType.DAILY;
    }

    ScheduleEntry(@NonNull CreateTaskLoader.WeeklyScheduleData weeklyScheduleData) {
        mDate = Date.today();
        mDayOfWeek = weeklyScheduleData.DayOfWeek;
        mTimePairPersist = new TimePairPersist(weeklyScheduleData.TimePair);
        mScheduleType = ScheduleType.WEEKLY;
    }

    ScheduleEntry(@Nullable CreateTaskActivity.ScheduleHint scheduleHint) {
        if (scheduleHint == null) { // new for task
            mDate = Date.today();
            mDayOfWeek = mDate.getDayOfWeek();
            mTimePairPersist = new TimePairPersist();
            mScheduleType = ScheduleType.SINGLE;
        } else if (scheduleHint.mTimePair != null) { // for instance group or instance join
            mDate = scheduleHint.mDate;
            mDayOfWeek = mDate.getDayOfWeek();
            mTimePairPersist = new TimePairPersist(scheduleHint.mTimePair);
            mScheduleType = ScheduleType.SINGLE;
        } else { // for group root
            mDate = scheduleHint.mDate;
            mDayOfWeek = mDate.getDayOfWeek();
            mTimePairPersist = new TimePairPersist();
            mScheduleType = ScheduleType.SINGLE;
        }
    }

    private ScheduleEntry(@NonNull Date date, @NonNull DayOfWeek dayOfWeek, boolean monthlyDay, int monthDayNumber, int monthWeekNumber, DayOfWeek monthWeekDay, boolean beginningOfMonth, @NonNull TimePairPersist timePairPersist, @NonNull ScheduleType scheduleType, @Nullable String error) { // replace with more specific
        mDate = date;
        mDayOfWeek = dayOfWeek;
        mMonthlyDay = monthlyDay;
        mMonthDayNumber = monthDayNumber;
        mMonthWeekNumber = monthWeekNumber;
        mMonthWeekDay = monthWeekDay;
        mBeginningOfMonth = beginningOfMonth;
        mTimePairPersist = timePairPersist;
        mScheduleType = scheduleType;
        mError = error;
    }

    ScheduleEntry(@NonNull ScheduleDialogFragment.ScheduleDialogData scheduleDialogData) {
        mDate = scheduleDialogData.mDate;
        mDayOfWeek = scheduleDialogData.mDayOfWeek;
        mMonthlyDay = scheduleDialogData.mMonthlyDay;
        mMonthDayNumber = scheduleDialogData.mMonthDayNumber;
        mMonthWeekNumber = scheduleDialogData.mMonthWeekNumber;
        mMonthWeekDay = scheduleDialogData.mMonthWeekDay;
        mBeginningOfMonth = scheduleDialogData.mBeginningOfMonth;
        mTimePairPersist = scheduleDialogData.mTimePairPersist;
        mScheduleType = scheduleDialogData.mScheduleType;
        mError = null;
    }

    @NonNull
    String getText(@NonNull Map<Integer, CreateTaskLoader.CustomTimeData> customTimeDatas, @NonNull Context context) {
        switch (mScheduleType) {
            case SINGLE:
                if (mTimePairPersist.getCustomTimeId() != null) {
                    CreateTaskLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePairPersist.getCustomTimeId());
                    Assert.assertTrue(customTimeData != null);

                    return mDate.getDisplayText(context) + ", " + customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDate.getDayOfWeek()) + ")";
                } else {
                    return mDate.getDisplayText(context) + ", " + mTimePairPersist.getHourMinute().toString();
                }
            case DAILY:
                if (mTimePairPersist.getCustomTimeId() != null) {
                    CreateTaskLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePairPersist.getCustomTimeId());
                    Assert.assertTrue(customTimeData != null);

                    return customTimeData.Name;
                } else {
                    return mTimePairPersist.getHourMinute().toString();
                }
            case WEEKLY:
                if (mTimePairPersist.getCustomTimeId() != null) {
                    CreateTaskLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePairPersist.getCustomTimeId());
                    Assert.assertTrue(customTimeData != null);

                    return mDayOfWeek + ", " + customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDayOfWeek) + ")";
                } else {
                    return mDayOfWeek + ", " + mTimePairPersist.getHourMinute().toString();
                }
            case MONTHLY_DAY: {
                Assert.assertTrue(mMonthlyDay);

                String day = mMonthDayNumber + " " + context.getString(R.string.monthDay) + " " + context.getString(R.string.monthDayStart) + " " + context.getResources().getStringArray(R.array.month)[mBeginningOfMonth ? 0 : 1] + " " + context.getString(R.string.monthDayEnd);

                if (mTimePairPersist.getCustomTimeId() != null) {
                    CreateTaskLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePairPersist.getCustomTimeId());
                    Assert.assertTrue(customTimeData != null);

                    return day + ", " + customTimeData.Name;
                } else {
                    return day + ", " + mTimePairPersist.getHourMinute();
                }
            }
            case MONTHLY_WEEK: {
                Assert.assertTrue(!mMonthlyDay);

                String day = mMonthWeekNumber + " " + mMonthWeekDay + " " + context.getString(R.string.monthDayStart) + " " + context.getResources().getStringArray(R.array.month)[mBeginningOfMonth ? 0 : 1] + " " + context.getString(R.string.monthDayEnd);

                if (mTimePairPersist.getCustomTimeId() != null) {
                    CreateTaskLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePairPersist.getCustomTimeId());
                    Assert.assertTrue(customTimeData != null);

                    return day + ", " + customTimeData.Name;
                } else {
                    return day + ", " + mTimePairPersist.getHourMinute();
                }
            }
            default:
                throw new UnsupportedOperationException();
        }
    }

    @NonNull
    CreateTaskLoader.ScheduleData getScheduleData() {
        switch (mScheduleType) {
            case SINGLE:
                return new CreateTaskLoader.SingleScheduleData(mDate, mTimePairPersist.getTimePair());
            case DAILY:
                return new CreateTaskLoader.DailyScheduleData(mTimePairPersist.getTimePair());
            case WEEKLY:
                return new CreateTaskLoader.WeeklyScheduleData(mDayOfWeek, mTimePairPersist.getTimePair());
            default:
                throw new UnsupportedOperationException();
        }
    }

    @NonNull
    ScheduleDialogFragment.ScheduleDialogData getScheduleDialogData(@NonNull Date today, @Nullable CreateTaskActivity.ScheduleHint scheduleHint) {
        switch (mScheduleType) {
            case SINGLE: {
                int monthDayNumber = mDate.getDay();
                boolean beginningOfMonth = true;
                if (monthDayNumber > 28) {
                    monthDayNumber = getDaysInMonth(mDate) - monthDayNumber + 1;
                    beginningOfMonth = false;
                }
                int monthWeekNumber = (monthDayNumber - 1) / 7 + 1;

                return new ScheduleDialogFragment.ScheduleDialogData(mDate, mDate.getDayOfWeek(), true, monthDayNumber, monthWeekNumber, mDate.getDayOfWeek(), beginningOfMonth, mTimePairPersist, ScheduleType.SINGLE);
            }
            case DAILY: {
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
            case WEEKLY: {
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
            case MONTHLY_DAY: {
                Date date = (scheduleHint != null ? scheduleHint.mDate : today);

                if (mBeginningOfMonth) {
                    date = new Date(date.getYear(), date.getMonth(), mMonthDayNumber);
                } else {
                    date = new Date(date.getYear(), date.getMonth(), getDaysInMonth(date) - mMonthDayNumber + 1);
                }

                return new ScheduleDialogFragment.ScheduleDialogData(date, date.getDayOfWeek(), true, mMonthDayNumber, (mMonthDayNumber - 1) / 7 + 1, date.getDayOfWeek(), mBeginningOfMonth, mTimePairPersist, ScheduleType.MONTHLY_DAY);
            }
            case MONTHLY_WEEK: {
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
            default: {
                throw new UnsupportedOperationException();
            }
        }
    }

    private int getDaysInMonth(@NonNull Date date) {
        Calendar calendar = new GregorianCalendar(date.getYear(), date.getMonth() - 1, 1);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(mDate, 0);
        parcel.writeSerializable(mDayOfWeek);
        parcel.writeInt(mMonthlyDay ? 1 : 0);
        parcel.writeInt(mMonthDayNumber);
        parcel.writeInt(mMonthWeekNumber);
        parcel.writeSerializable(mMonthWeekDay);
        parcel.writeInt(mBeginningOfMonth ? 1 : 0);
        parcel.writeParcelable(mTimePairPersist, 0);
        parcel.writeSerializable(mScheduleType);
        parcel.writeString(mError);
    }

    @SuppressWarnings("unused")
    public static final Creator<ScheduleEntry> CREATOR = new Creator<ScheduleEntry>() {
        @Override
        public ScheduleEntry createFromParcel(Parcel in) {
            Date date = in.readParcelable(Date.class.getClassLoader());
            Assert.assertTrue(date != null);

            DayOfWeek dayOfWeek = (DayOfWeek) in.readSerializable();
            Assert.assertTrue(dayOfWeek != null);

            boolean monthlyDay = (in.readInt() == 1);

            int monthDayNumber = in.readInt();

            int monthWeekNumber = in.readInt();

            DayOfWeek monthWeekDay = (DayOfWeek) in.readSerializable();
            Assert.assertTrue(monthWeekDay != null);

            boolean beginningOfMonth = (in.readInt() == 1);

            TimePairPersist timePairPersist = in.readParcelable(TimePairPersist.class.getClassLoader());
            Assert.assertTrue(timePairPersist != null);

            ScheduleType scheduleType = (ScheduleType) in.readSerializable();
            Assert.assertTrue(scheduleType != null);

            String error = in.readString();

            return new ScheduleEntry(date, dayOfWeek, monthlyDay, monthDayNumber, monthWeekNumber, monthWeekDay, beginningOfMonth, timePairPersist, scheduleType, error);
        }

        @Override
        public ScheduleEntry[] newArray(int size) {
            return new ScheduleEntry[size];
        }
    };
}
