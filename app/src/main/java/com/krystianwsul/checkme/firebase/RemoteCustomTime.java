package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.krystianwsul.checkme.domainmodel.CustomTime;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.firebase.records.RemoteCustomTimeRecord;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;

import java.util.TreeMap;

class RemoteCustomTime implements CustomTime {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final RemoteProject mRemoteProject;

    @NonNull
    private final RemoteCustomTimeRecord mRemoteCustomTimeRecord;

    RemoteCustomTime(@NonNull DomainFactory domainFactory, @NonNull RemoteProject remoteProject, @NonNull RemoteCustomTimeRecord remoteCustomTimeRecord) {
        mDomainFactory = domainFactory;
        mRemoteProject = remoteProject;
        mRemoteCustomTimeRecord = remoteCustomTimeRecord;
    }

    @NonNull
    public String getId() {
        return mRemoteCustomTimeRecord.getId();
    }

    @NonNull
    @Override
    public String getName() {
        return mRemoteCustomTimeRecord.getName();
    }

    @NonNull
    @Override
    public HourMinute getHourMinute(@NonNull DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case SUNDAY:
                return new HourMinute(mRemoteCustomTimeRecord.getSundayHour(), mRemoteCustomTimeRecord.getSundayMinute());
            case MONDAY:
                return new HourMinute(mRemoteCustomTimeRecord.getMondayHour(), mRemoteCustomTimeRecord.getMondayMinute());
            case TUESDAY:
                return new HourMinute(mRemoteCustomTimeRecord.getTuesdayHour(), mRemoteCustomTimeRecord.getTuesdayMinute());
            case WEDNESDAY:
                return new HourMinute(mRemoteCustomTimeRecord.getWednesdayHour(), mRemoteCustomTimeRecord.getWednesdayMinute());
            case THURSDAY:
                return new HourMinute(mRemoteCustomTimeRecord.getThursdayHour(), mRemoteCustomTimeRecord.getThursdayMinute());
            case FRIDAY:
                return new HourMinute(mRemoteCustomTimeRecord.getFridayHour(), mRemoteCustomTimeRecord.getFridayMinute());
            case SATURDAY:
                return new HourMinute(mRemoteCustomTimeRecord.getSaturdayHour(), mRemoteCustomTimeRecord.getSaturdayMinute());
            default:
                throw new IllegalArgumentException("invalid day: " + dayOfWeek);
        }
    }

    @NonNull
    @Override
    public TreeMap<DayOfWeek, HourMinute> getHourMinutes() {
        TreeMap<DayOfWeek, HourMinute> hourMinutes = new TreeMap<>();
        for (DayOfWeek dayOfWeek : DayOfWeek.values())
            hourMinutes.put(dayOfWeek, getHourMinute(dayOfWeek));
        return hourMinutes;
    }

    @NonNull
    public String toString() {
        return getName();
    }

    @NonNull
    @Override
    public Pair<CustomTime, HourMinute> getPair() {
        return new Pair<>(this, null);
    }

    @NonNull
    @Override
    public TimePair getTimePair() {
        return new TimePair(new CustomTimeKey(mRemoteProject.getId(), mRemoteCustomTimeRecord.getId()), null);
    }

    @NonNull
    @Override
    public CustomTimeKey getCustomTimeKey() {
        return mDomainFactory.getCustomTimeKey(mRemoteProject.getId(), getId());
    }
}
