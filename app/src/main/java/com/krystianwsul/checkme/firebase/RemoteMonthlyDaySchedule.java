package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.MergedInstance;
import com.krystianwsul.checkme.firebase.records.RemoteMonthlyDayScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteScheduleRecord;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.Utils;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMilli;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

class RemoteMonthlyDaySchedule extends RemoteSchedule {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final RemoteMonthlyDayScheduleRecord mRemoteMonthlyDayScheduleRecord;

    RemoteMonthlyDaySchedule(@NonNull DomainFactory domainFactory, @NonNull RemoteMonthlyDayScheduleRecord remoteMonthlyDaySchedulRecord) {
        mDomainFactory = domainFactory;
        mRemoteMonthlyDayScheduleRecord = remoteMonthlyDaySchedulRecord;
    }

    @NonNull
    @Override
    protected RemoteScheduleRecord getRemoteScheduleRecord() {
        return mRemoteMonthlyDayScheduleRecord;
    }

    @NonNull
    @Override
    String getScheduleText(@NonNull Context context) {
        String day = mRemoteMonthlyDayScheduleRecord.getDayOfMonth() + " " + context.getString(R.string.monthDay) + " " + context.getString(R.string.monthDayStart) + " " + context.getResources().getStringArray(R.array.month)[mRemoteMonthlyDayScheduleRecord.getBeginningOfMonth() ? 0 : 1] + " " + context.getString(R.string.monthDayEnd);

        return day + ": " + getTime();
    }

    @NonNull
    private Time getTime() {
        Assert.assertTrue(mRemoteMonthlyDayScheduleRecord.getCustomTimeId() == null); // todo customtime
        Assert.assertTrue(mRemoteMonthlyDayScheduleRecord.getHour() != null);
        Assert.assertTrue(mRemoteMonthlyDayScheduleRecord.getMinute() != null);

        //Integer customTimeId = mMonthlyDayScheduleRecord.getCustomTimeId();
        //if (customTimeId != null) {
        //    return mDomainFactory.getCustomTime(mMonthlyDayScheduleRecord.getCustomTimeId());
        //} else {
        Integer hour = mRemoteMonthlyDayScheduleRecord.getHour();
        Integer minute = mRemoteMonthlyDayScheduleRecord.getMinute();
        Assert.assertTrue(hour != null);
        Assert.assertTrue(minute != null);
        return new NormalTime(hour, minute);
        //}
    }

    @Override
    public void setEndExactTimeStamp(@NonNull ExactTimeStamp now) {
        Assert.assertTrue(current(now));

        mRemoteMonthlyDayScheduleRecord.setEndTime(now.getLong());
    }

    @NonNull
    @Override
    List<MergedInstance> getInstances(@NonNull RemoteTask task, ExactTimeStamp givenStartExactTimeStamp, @NonNull ExactTimeStamp givenExactEndTimeStamp) {
        ExactTimeStamp myStartTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp myEndTimeStamp = getEndExactTimeStamp();

        List<MergedInstance> instances = new ArrayList<>();

        ExactTimeStamp startExactTimeStamp;
        ExactTimeStamp endExactTimeStamp;

        if (givenStartExactTimeStamp == null || (givenStartExactTimeStamp.compareTo(myStartTimeStamp) < 0))
            startExactTimeStamp = myStartTimeStamp;
        else
            startExactTimeStamp = givenStartExactTimeStamp;

        if (myEndTimeStamp == null || (myEndTimeStamp.compareTo(givenExactEndTimeStamp) > 0))
            endExactTimeStamp = givenExactEndTimeStamp;
        else
            endExactTimeStamp = myEndTimeStamp;

        if (startExactTimeStamp.compareTo(endExactTimeStamp) >= 0)
            return instances;

        Assert.assertTrue(startExactTimeStamp.compareTo(endExactTimeStamp) < 0);

        if (startExactTimeStamp.getDate().equals(endExactTimeStamp.getDate())) {
            instances.add(getInstanceInDate(task, startExactTimeStamp.getDate(), startExactTimeStamp.getHourMilli(), endExactTimeStamp.getHourMilli()));
        } else {
            instances.add(getInstanceInDate(task, startExactTimeStamp.getDate(), startExactTimeStamp.getHourMilli(), null));

            Calendar loopStartCalendar = startExactTimeStamp.getDate().getCalendar();
            loopStartCalendar.add(Calendar.DATE, 1);
            Calendar loopEndCalendar = endExactTimeStamp.getDate().getCalendar();

            for (; loopStartCalendar.before(loopEndCalendar); loopStartCalendar.add(Calendar.DATE, 1))
                instances.add(getInstanceInDate(task, new Date(loopStartCalendar), null, null));

            instances.add(getInstanceInDate(task, endExactTimeStamp.getDate(), null, endExactTimeStamp.getHourMilli()));
        }

        return Stream.of(instances)
                .filter(instance -> instance != null)
                .collect(Collectors.toList());
    }

    @Nullable
    protected MergedInstance getInstanceInDate(@NonNull RemoteTask task, @NonNull Date date, @Nullable HourMilli startHourMilli, @Nullable HourMilli endHourMilli) {
        Date dateThisMonth = getDate(date.getYear(), date.getMonth());

        if (!dateThisMonth.equals(date))
            return null;

        HourMinute hourMinute = getTime().getHourMinute(date.getDayOfWeek());
        Assert.assertTrue(hourMinute != null);

        if (startHourMilli != null && startHourMilli.compareTo(hourMinute.toHourMilli()) > 0)
            return null;

        if (endHourMilli != null && endHourMilli.compareTo(hourMinute.toHourMilli()) <= 0)
            return null;

        DateTime scheduleDateTime = new DateTime(date, getTime());
        Assert.assertTrue(task.current(scheduleDateTime.getTimeStamp().toExactTimeStamp()));

        return mDomainFactory.getInstance(task, scheduleDateTime);
    }

    @NonNull
    private Date getDate(int year, int month) {
        return Utils.getDateInMonth(year, month, mRemoteMonthlyDayScheduleRecord.getDayOfMonth(), mRemoteMonthlyDayScheduleRecord.getBeginningOfMonth());
    }

    @Nullable
    @Override
    public Integer getCustomTimeId() {
        return mRemoteMonthlyDayScheduleRecord.getCustomTimeId();
    }

    @NonNull
    @Override
    public ScheduleType getType() {
        return ScheduleType.MONTHLY_DAY;
    }

    @Nullable
    @Override
    public TimeStamp getNextAlarm(@NonNull ExactTimeStamp now) {
        Date today = now.getDate();

        Date dateThisMonth = getDate(today.getYear(), today.getMonth());
        Time time = getTime();
        TimeStamp thisMonth = new DateTime(dateThisMonth, time).getTimeStamp();

        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        if (thisMonth.toExactTimeStamp().compareTo(now) > 0) {
            if (endExactTimeStamp != null && endExactTimeStamp.compareTo(thisMonth.toExactTimeStamp()) <= 0)
                return null;
            else
                return thisMonth;
        } else {
            Calendar calendar = now.getCalendar();
            calendar.add(Calendar.MONTH, 1);

            Date dateNextMonth = new Date(calendar);

            TimeStamp nextMonth = new DateTime(dateNextMonth, getTime()).getTimeStamp();

            if (endExactTimeStamp != null && endExactTimeStamp.compareTo(nextMonth.toExactTimeStamp()) <= 0)
                return null;
            else
                return nextMonth;
        }
    }
}
