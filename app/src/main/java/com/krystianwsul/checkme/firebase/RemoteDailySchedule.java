package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.MergedInstance;
import com.krystianwsul.checkme.domainmodel.Task;
import com.krystianwsul.checkme.firebase.records.RemoteDailyScheduleRecord;
import com.krystianwsul.checkme.firebase.records.RemoteScheduleRecord;
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

class RemoteDailySchedule extends RemoteSchedule {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final RemoteDailyScheduleRecord mRemoteDailyScheduleRecord;

    RemoteDailySchedule(@NonNull DomainFactory domainFactory, @NonNull RemoteDailyScheduleRecord remoteDailyScheduleRecord) {
        mDomainFactory = domainFactory;
        mRemoteDailyScheduleRecord = remoteDailyScheduleRecord;
    }

    @NonNull
    @Override
    protected RemoteScheduleRecord getRemoteScheduleRecord() {
        return mRemoteDailyScheduleRecord;
    }

    @NonNull
    @Override
    public String getScheduleText(@NonNull Context context) {
        return context.getString(R.string.daily) + " " + getTime().toString();
    }

    @NonNull
    Time getTime() {
        Assert.assertTrue(mRemoteDailyScheduleRecord.getCustomTimeId() == null); // todo customtime
        Assert.assertTrue(mRemoteDailyScheduleRecord.getHour() != null);
        Assert.assertTrue(mRemoteDailyScheduleRecord.getMinute() != null);

        //Integer customTimeId = mDailyScheduleRecord.getCustomTimeId();
        //if (customTimeId != null) {
        //    return mDomainFactory.getCustomTime(mDailyScheduleRecord.getCustomTimeId());
        //} else {
        Integer hour = mRemoteDailyScheduleRecord.getHour();
        Integer minute = mRemoteDailyScheduleRecord.getMinute();
        Assert.assertTrue(hour != null);
        Assert.assertTrue(minute != null);
        return new NormalTime(hour, minute);
        //}
    }

    @Override
    public void setEndExactTimeStamp(@NonNull ExactTimeStamp now) {
        Assert.assertTrue(current(now));

        mRemoteDailyScheduleRecord.setEndTime(now.getLong());
    }

    @NonNull
    @Override
    public List<MergedInstance> getInstances(@NonNull Task task, ExactTimeStamp givenStartExactTimeStamp, @NonNull ExactTimeStamp givenExactEndTimeStamp) {
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
    protected MergedInstance getInstanceInDate(@NonNull Task task, @NonNull Date date, @Nullable HourMilli startHourMilli, @Nullable HourMilli endHourMilli) {
        DayOfWeek day = date.getDayOfWeek();

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
        return mRemoteDailyScheduleRecord.getCustomTimeId();
    }

    @NonNull
    @Override
    public ScheduleType getType() {
        return ScheduleType.DAILY;
    }

    @Nullable
    @Override
    public TimeStamp getNextAlarm(@NonNull ExactTimeStamp now) {
        Date today = Date.today();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date tomorrow = new Date(calendar);

        DayOfWeek dayOfWeek = today.getDayOfWeek();
        HourMinute nowHourMinute = new HourMinute(now.getCalendar());

        HourMinute dailyScheduleHourMinute = getTime().getHourMinute(dayOfWeek);

        DateTime dailyScheduleDateTime;
        if (dailyScheduleHourMinute.compareTo(nowHourMinute) > 0)
            dailyScheduleDateTime = new DateTime(today, getTime());
        else
            dailyScheduleDateTime = new DateTime(tomorrow, getTime());

        return dailyScheduleDateTime.getTimeStamp();
    }
}
