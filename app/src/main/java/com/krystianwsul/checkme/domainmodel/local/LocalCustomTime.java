package com.krystianwsul.checkme.domainmodel.local;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.CustomTime;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.firebase.records.RemoteCustomTimeRecord;
import com.krystianwsul.checkme.persistencemodel.CustomTimeRecord;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class LocalCustomTime implements CustomTime {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final CustomTimeRecord mCustomTimeRecord;

    @NonNull
    private final Map<String, RemoteCustomTimeRecord> mRemoteCustomTimeRecords = new HashMap<>();

    LocalCustomTime(@NonNull DomainFactory domainFactory, @NonNull CustomTimeRecord customTimeRecord) {
        mDomainFactory = domainFactory;
        mCustomTimeRecord = customTimeRecord;
    }

    @NonNull
    @Override
    public String getName() {
        return mCustomTimeRecord.getName();
    }

    public void setName(@NonNull String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        mCustomTimeRecord.setName(name);

        for (RemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteCustomTimeRecords.values())
            remoteCustomTimeRecord.setName(name);
    }

    @NonNull
    @Override
    public HourMinute getHourMinute(@NonNull DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case SUNDAY:
                return new HourMinute(mCustomTimeRecord.getSundayHour(), mCustomTimeRecord.getSundayMinute());
            case MONDAY:
                return new HourMinute(mCustomTimeRecord.getMondayHour(), mCustomTimeRecord.getMondayMinute());
            case TUESDAY:
                return new HourMinute(mCustomTimeRecord.getTuesdayHour(), mCustomTimeRecord.getTuesdayMinute());
            case WEDNESDAY:
                return new HourMinute(mCustomTimeRecord.getWednesdayHour(), mCustomTimeRecord.getWednesdayMinute());
            case THURSDAY:
                return new HourMinute(mCustomTimeRecord.getThursdayHour(), mCustomTimeRecord.getThursdayMinute());
            case FRIDAY:
                return new HourMinute(mCustomTimeRecord.getFridayHour(), mCustomTimeRecord.getFridayMinute());
            case SATURDAY:
                return new HourMinute(mCustomTimeRecord.getSaturdayHour(), mCustomTimeRecord.getSaturdayMinute());
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

    public void setHourMinute(@NonNull DayOfWeek dayOfWeek, @NonNull HourMinute hourMinute) {
        switch (dayOfWeek) {
            case SUNDAY:
                mCustomTimeRecord.setSundayHour(hourMinute.getHour());
                mCustomTimeRecord.setSundayMinute(hourMinute.getMinute());

                for (RemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteCustomTimeRecords.values()) {
                    remoteCustomTimeRecord.setSundayHour(hourMinute.getHour());
                    remoteCustomTimeRecord.setSundayMinute(hourMinute.getMinute());
                }

                break;
            case MONDAY:
                mCustomTimeRecord.setMondayHour(hourMinute.getHour());
                mCustomTimeRecord.setMondayMinute(hourMinute.getMinute());

                for (RemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteCustomTimeRecords.values()) {
                    remoteCustomTimeRecord.setMondayHour(hourMinute.getHour());
                    remoteCustomTimeRecord.setMondayMinute(hourMinute.getMinute());
                }

                break;
            case TUESDAY:
                mCustomTimeRecord.setTuesdayHour(hourMinute.getHour());
                mCustomTimeRecord.setTuesdayMinute(hourMinute.getMinute());

                for (RemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteCustomTimeRecords.values()) {
                    remoteCustomTimeRecord.setTuesdayHour(hourMinute.getHour());
                    remoteCustomTimeRecord.setTuesdayMinute(hourMinute.getMinute());
                }

                break;
            case WEDNESDAY:
                mCustomTimeRecord.setWednesdayHour(hourMinute.getHour());
                mCustomTimeRecord.setWednesdayMinute(hourMinute.getMinute());

                for (RemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteCustomTimeRecords.values()) {
                    remoteCustomTimeRecord.setWednesdayHour(hourMinute.getHour());
                    remoteCustomTimeRecord.setWednesdayMinute(hourMinute.getMinute());
                }

                break;
            case THURSDAY:
                mCustomTimeRecord.setThursdayHour(hourMinute.getHour());
                mCustomTimeRecord.setThursdayMinute(hourMinute.getMinute());

                for (RemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteCustomTimeRecords.values()) {
                    remoteCustomTimeRecord.setThursdayHour(hourMinute.getHour());
                    remoteCustomTimeRecord.setThursdayMinute(hourMinute.getMinute());
                }

                break;
            case FRIDAY:
                mCustomTimeRecord.setFridayHour(hourMinute.getHour());
                mCustomTimeRecord.setFridayMinute(hourMinute.getMinute());

                for (RemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteCustomTimeRecords.values()) {
                    remoteCustomTimeRecord.setFridayHour(hourMinute.getHour());
                    remoteCustomTimeRecord.setFridayMinute(hourMinute.getMinute());
                }

                break;
            case SATURDAY:
                mCustomTimeRecord.setSaturdayHour(hourMinute.getHour());
                mCustomTimeRecord.setSaturdayMinute(hourMinute.getMinute());

                for (RemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteCustomTimeRecords.values()) {
                    remoteCustomTimeRecord.setSaturdayHour(hourMinute.getHour());
                    remoteCustomTimeRecord.setSaturdayMinute(hourMinute.getMinute());
                }

                break;
            default:
                throw new IllegalArgumentException("invalid day: " + dayOfWeek);
        }
    }

    @NonNull
    public String toString() {
        return getName();
    }

    public int getId() {
        return mCustomTimeRecord.getId();
    }

    @NonNull
    @Override
    public Pair<CustomTime, HourMinute> getPair() {
        return new Pair<>(this, null);
    }

    public boolean getCurrent() {
        return mCustomTimeRecord.getCurrent();
    }

    public void setCurrent() {
        mCustomTimeRecord.setCurrent(false);
    }

    @NonNull
    @Override
    public TimePair getTimePair() {
        return new TimePair(new CustomTimeKey(mCustomTimeRecord.getId()), null);
    }

    public void delete() {
        mDomainFactory.getLocalFactory().deleteCustomTime(this);

        mCustomTimeRecord.delete();
    }

    @NonNull
    @Override
    public CustomTimeKey getCustomTimeKey() {
        return new CustomTimeKey(getId());
    }

    public void addRemoteCustomTimeRecord(@NonNull RemoteCustomTimeRecord remoteCustomTimeRecord) {
        Assert.assertTrue(remoteCustomTimeRecord.getLocalId() == mCustomTimeRecord.getId());
        Assert.assertTrue(!mRemoteCustomTimeRecords.containsKey(remoteCustomTimeRecord.getProjectId()));

        mRemoteCustomTimeRecords.put(remoteCustomTimeRecord.getProjectId(), remoteCustomTimeRecord);

        // bez zapisywania na razie, dopiero przy następnej okazji
        if (!remoteCustomTimeRecord.getName().equals(mCustomTimeRecord.getName()))
            remoteCustomTimeRecord.setName(mCustomTimeRecord.getName());

        if (remoteCustomTimeRecord.getSundayHour() != mCustomTimeRecord.getSundayHour())
            remoteCustomTimeRecord.setSundayHour(mCustomTimeRecord.getSundayHour());

        if (remoteCustomTimeRecord.getSundayMinute() != mCustomTimeRecord.getSundayMinute())
            remoteCustomTimeRecord.setSundayMinute(mCustomTimeRecord.getSundayMinute());

        if (remoteCustomTimeRecord.getMondayHour() != mCustomTimeRecord.getMondayHour())
            remoteCustomTimeRecord.setMondayHour(mCustomTimeRecord.getMondayHour());

        if (remoteCustomTimeRecord.getMondayMinute() != mCustomTimeRecord.getMondayMinute())
            remoteCustomTimeRecord.setMondayMinute(mCustomTimeRecord.getMondayMinute());

        if (remoteCustomTimeRecord.getTuesdayHour() != mCustomTimeRecord.getTuesdayHour())
            remoteCustomTimeRecord.setTuesdayHour(mCustomTimeRecord.getTuesdayHour());

        if (remoteCustomTimeRecord.getTuesdayMinute() != mCustomTimeRecord.getTuesdayMinute())
            remoteCustomTimeRecord.setTuesdayMinute(mCustomTimeRecord.getTuesdayMinute());

        if (remoteCustomTimeRecord.getWednesdayHour() != mCustomTimeRecord.getWednesdayHour())
            remoteCustomTimeRecord.setWednesdayHour(mCustomTimeRecord.getWednesdayHour());

        if (remoteCustomTimeRecord.getWednesdayMinute() != mCustomTimeRecord.getWednesdayMinute())
            remoteCustomTimeRecord.setWednesdayMinute(mCustomTimeRecord.getWednesdayMinute());

        if (remoteCustomTimeRecord.getThursdayHour() != mCustomTimeRecord.getThursdayHour())
            remoteCustomTimeRecord.setThursdayHour(mCustomTimeRecord.getThursdayHour());

        if (remoteCustomTimeRecord.getThursdayMinute() != mCustomTimeRecord.getThursdayMinute())
            remoteCustomTimeRecord.setThursdayMinute(mCustomTimeRecord.getThursdayMinute());

        if (remoteCustomTimeRecord.getFridayHour() != mCustomTimeRecord.getFridayHour())
            remoteCustomTimeRecord.setFridayHour(mCustomTimeRecord.getFridayHour());

        if (remoteCustomTimeRecord.getFridayMinute() != mCustomTimeRecord.getFridayMinute())
            remoteCustomTimeRecord.setFridayMinute(mCustomTimeRecord.getFridayMinute());

        if (remoteCustomTimeRecord.getSaturdayHour() != mCustomTimeRecord.getSaturdayHour())
            remoteCustomTimeRecord.setSaturdayHour(mCustomTimeRecord.getSaturdayHour());

        if (remoteCustomTimeRecord.getSaturdayMinute() != mCustomTimeRecord.getSaturdayMinute())
            remoteCustomTimeRecord.setSaturdayMinute(mCustomTimeRecord.getSaturdayMinute());
    }

    public boolean hasRemoteRecord(@NonNull String projectId) {
        Assert.assertTrue(!TextUtils.isEmpty(projectId));

        return mRemoteCustomTimeRecords.containsKey(projectId);
    }

    void clearRemoteRecords() {
        mRemoteCustomTimeRecords.clear();
    }

    @NonNull
    public String getRemoteId(@NonNull String projectId) {
        Assert.assertTrue(!TextUtils.isEmpty(projectId));
        Assert.assertTrue(mRemoteCustomTimeRecords.containsKey(projectId));

        RemoteCustomTimeRecord remoteCustomTimeRecord = mRemoteCustomTimeRecords.get(projectId);
        Assert.assertTrue(remoteCustomTimeRecord != null);

        return remoteCustomTimeRecord.getId();
    }
}
