package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.MergedInstance;
import com.krystianwsul.checkme.firebase.records.RemoteScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteWeeklyScheduleRecord;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
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

class RemoteWeeklySchedule extends RemoteSchedule {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final RemoteWeeklyScheduleRecord mRemoteWeeklyScheduleRecord;

    RemoteWeeklySchedule(@NonNull DomainFactory domainFactory, @NonNull RemoteWeeklyScheduleRecord remoteWeeklyScheduleRecord) {
        mDomainFactory = domainFactory;
        mRemoteWeeklyScheduleRecord = remoteWeeklyScheduleRecord;
    }

    @NonNull
    @Override
    protected RemoteScheduleRecord getRemoteScheduleRecord() {
        return mRemoteWeeklyScheduleRecord;
    }

    @NonNull
    @Override
    String getScheduleText(@NonNull Context context) {
        return getDayOfWeek() + ": " + getTime();
    }

    @NonNull
    private DayOfWeek getDayOfWeek() {
        DayOfWeek dayOfWeek = DayOfWeek.values()[mRemoteWeeklyScheduleRecord.getDayOfWeek()];
        Assert.assertTrue(dayOfWeek != null);

        return dayOfWeek;
    }

    @NonNull
    private Time getTime() {
        Assert.assertTrue(mRemoteWeeklyScheduleRecord.getCustomTimeId() == null); // todo customtime
        Assert.assertTrue(mRemoteWeeklyScheduleRecord.getHour() != null);
        Assert.assertTrue(mRemoteWeeklyScheduleRecord.getMinute() != null);

        //Integer customTimeId = mWeeklyScheduleRecord.getCustomTimeId();
        //if (customTimeId != null) {
        //    return mDomainFactory.getCustomTime(mWeeklyScheduleRecord.getCustomTimeId());
        //} else {
        Integer hour = mRemoteWeeklyScheduleRecord.getHour();
        Integer minute = mRemoteWeeklyScheduleRecord.getMinute();
        Assert.assertTrue(hour != null);
        Assert.assertTrue(minute != null);
        return new NormalTime(hour, minute);
        //}
    }

    @Override
    public void setEndExactTimeStamp(@NonNull ExactTimeStamp now) {
        Assert.assertTrue(current(now));

        mRemoteWeeklyScheduleRecord.setEndTime(now.getLong());
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
        DayOfWeek day = date.getDayOfWeek();

        if (getDayOfWeek() != day)
            return null;

        HourMinute hourMinute = getTime().getHourMinute(day);
        Assert.assertTrue(hourMinute != null);

        if (startHourMilli != null && startHourMilli.compareTo(hourMinute.toHourMilli()) > 0)
            return null;

        if (endHourMilli != null && endHourMilli.compareTo(hourMinute.toHourMilli()) <= 0)
            return null;

        DateTime scheduleDateTime = new DateTime(date, getTime());
        Assert.assertTrue(task.current(scheduleDateTime.getTimeStamp().toExactTimeStamp()));

        return mDomainFactory.getInstance(task, scheduleDateTime);
    }

    @Nullable
    @Override
    public Integer getCustomTimeId() {
        return mRemoteWeeklyScheduleRecord.getCustomTimeId();
    }

    @NonNull
    @Override
    public ScheduleType getType() {
        return ScheduleType.WEEKLY;
    }

    @NonNull
    @Override
    public TimeStamp getNextAlarm(@NonNull ExactTimeStamp now) {
        Date today = Date.today();

        DayOfWeek dayOfWeek = today.getDayOfWeek();
        HourMinute nowHourMinute = new HourMinute(now.getCalendar());

        int ordinalDifference = (getDayOfWeek().ordinal() - dayOfWeek.ordinal());
        Calendar thisCalendar = today.getCalendar();
        if ((ordinalDifference > 0) || ((ordinalDifference == 0) && getTime().getHourMinute(dayOfWeek).compareTo(nowHourMinute) > 0))
            thisCalendar.add(Calendar.DAY_OF_WEEK, ordinalDifference);
        else
            thisCalendar.add(Calendar.DAY_OF_WEEK, ordinalDifference + 7);
        Date thisDate = new Date(thisCalendar);

        return (new DateTime(thisDate, getTime())).getTimeStamp();
    }
}
