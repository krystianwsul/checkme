package com.krystianwsul.checkme.domainmodel.local;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.CustomTime;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.firebase.records.NewRemoteCustomTimeRecord;
import com.krystianwsul.checkme.firebase.records.RemoteCustomTimeRecord;
import com.krystianwsul.checkme.persistencemodel.CustomTimeRecord;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class LocalCustomTime implements CustomTime {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final CustomTimeRecord mCustomTimeRecord;

    @Nullable
    private RemoteCustomTimeRecord mRemoteCustomTimeRecord;

    @NonNull
    private final Map<String, NewRemoteCustomTimeRecord> mRemoteCustomTimeRecords = new HashMap<>();

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

        if (mRemoteCustomTimeRecord != null)
            mRemoteCustomTimeRecord.setName(name);

        for (NewRemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteCustomTimeRecords.values())
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

                if (mRemoteCustomTimeRecord != null) {
                    mRemoteCustomTimeRecord.setSundayHour(hourMinute.getHour());
                    mRemoteCustomTimeRecord.setSundayMinute(hourMinute.getMinute());
                }

                for (NewRemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteCustomTimeRecords.values()) {
                    remoteCustomTimeRecord.setSundayHour(hourMinute.getHour());
                    remoteCustomTimeRecord.setSundayMinute(hourMinute.getMinute());
                }

                break;
            case MONDAY:
                mCustomTimeRecord.setMondayHour(hourMinute.getHour());
                mCustomTimeRecord.setMondayMinute(hourMinute.getMinute());

                if (mRemoteCustomTimeRecord != null) {
                    mRemoteCustomTimeRecord.setMondayHour(hourMinute.getHour());
                    mRemoteCustomTimeRecord.setMondayMinute(hourMinute.getMinute());
                }

                for (NewRemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteCustomTimeRecords.values()) {
                    remoteCustomTimeRecord.setMondayHour(hourMinute.getHour());
                    remoteCustomTimeRecord.setMondayMinute(hourMinute.getMinute());
                }

                break;
            case TUESDAY:
                mCustomTimeRecord.setTuesdayHour(hourMinute.getHour());
                mCustomTimeRecord.setTuesdayMinute(hourMinute.getMinute());

                if (mRemoteCustomTimeRecord != null) {
                    mRemoteCustomTimeRecord.setTuesdayHour(hourMinute.getHour());
                    mRemoteCustomTimeRecord.setTuesdayMinute(hourMinute.getMinute());
                }

                for (NewRemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteCustomTimeRecords.values()) {
                    remoteCustomTimeRecord.setTuesdayHour(hourMinute.getHour());
                    remoteCustomTimeRecord.setTuesdayMinute(hourMinute.getMinute());
                }

                break;
            case WEDNESDAY:
                mCustomTimeRecord.setWednesdayHour(hourMinute.getHour());
                mCustomTimeRecord.setWednesdayMinute(hourMinute.getMinute());

                if (mRemoteCustomTimeRecord != null) {
                    mRemoteCustomTimeRecord.setWednesdayHour(hourMinute.getHour());
                    mRemoteCustomTimeRecord.setWednesdayMinute(hourMinute.getMinute());
                }

                for (NewRemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteCustomTimeRecords.values()) {
                    remoteCustomTimeRecord.setWednesdayHour(hourMinute.getHour());
                    remoteCustomTimeRecord.setWednesdayMinute(hourMinute.getMinute());
                }

                break;
            case THURSDAY:
                mCustomTimeRecord.setThursdayHour(hourMinute.getHour());
                mCustomTimeRecord.setThursdayMinute(hourMinute.getMinute());

                if (mRemoteCustomTimeRecord != null) {
                    mRemoteCustomTimeRecord.setThursdayHour(hourMinute.getHour());
                    mRemoteCustomTimeRecord.setThursdayMinute(hourMinute.getMinute());
                }

                for (NewRemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteCustomTimeRecords.values()) {
                    remoteCustomTimeRecord.setThursdayHour(hourMinute.getHour());
                    remoteCustomTimeRecord.setThursdayMinute(hourMinute.getMinute());
                }

                break;
            case FRIDAY:
                mCustomTimeRecord.setFridayHour(hourMinute.getHour());
                mCustomTimeRecord.setFridayMinute(hourMinute.getMinute());

                if (mRemoteCustomTimeRecord != null) {
                    mRemoteCustomTimeRecord.setFridayHour(hourMinute.getHour());
                    mRemoteCustomTimeRecord.setFridayMinute(hourMinute.getMinute());
                }

                for (NewRemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteCustomTimeRecords.values()) {
                    remoteCustomTimeRecord.setFridayHour(hourMinute.getHour());
                    remoteCustomTimeRecord.setFridayMinute(hourMinute.getMinute());
                }

                break;
            case SATURDAY:
                mCustomTimeRecord.setSaturdayHour(hourMinute.getHour());
                mCustomTimeRecord.setSaturdayMinute(hourMinute.getMinute());

                if (mRemoteCustomTimeRecord != null) {
                    mRemoteCustomTimeRecord.setSaturdayHour(hourMinute.getHour());
                    mRemoteCustomTimeRecord.setSaturdayMinute(hourMinute.getMinute());
                }

                for (NewRemoteCustomTimeRecord remoteCustomTimeRecord : mRemoteCustomTimeRecords.values()) {
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

        if (mRemoteCustomTimeRecord != null)
            mRemoteCustomTimeRecord.delete();

        Stream.of(mRemoteCustomTimeRecords.values())
                .forEach(NewRemoteCustomTimeRecord::delete);
    }

    @NonNull
    @Override
    public CustomTimeKey getCustomTimeKey() {
        return new CustomTimeKey(getId());
    }

    public void setRemoteCustomTimeRecord(@NonNull RemoteCustomTimeRecord remoteCustomTimeRecord) {
        Assert.assertTrue(remoteCustomTimeRecord.getLocalId() == mCustomTimeRecord.getId());

        mRemoteCustomTimeRecord = remoteCustomTimeRecord;

        // bez zapisywania na razie, dopiero przy następnej okazji
        if (!mRemoteCustomTimeRecord.getName().equals(mCustomTimeRecord.getName()))
            mRemoteCustomTimeRecord.setName(mCustomTimeRecord.getName());

        if (mRemoteCustomTimeRecord.getSundayHour() != mCustomTimeRecord.getSundayHour())
            mRemoteCustomTimeRecord.setSundayHour(mCustomTimeRecord.getSundayHour());

        if (mRemoteCustomTimeRecord.getSundayMinute() != mCustomTimeRecord.getSundayMinute())
            mRemoteCustomTimeRecord.setSundayMinute(mCustomTimeRecord.getSundayMinute());

        if (mRemoteCustomTimeRecord.getMondayHour() != mCustomTimeRecord.getMondayHour())
            mRemoteCustomTimeRecord.setMondayHour(mCustomTimeRecord.getMondayHour());

        if (mRemoteCustomTimeRecord.getMondayMinute() != mCustomTimeRecord.getMondayMinute())
            mRemoteCustomTimeRecord.setMondayMinute(mCustomTimeRecord.getMondayMinute());

        if (mRemoteCustomTimeRecord.getTuesdayHour() != mCustomTimeRecord.getTuesdayHour())
            mRemoteCustomTimeRecord.setTuesdayHour(mCustomTimeRecord.getTuesdayHour());

        if (mRemoteCustomTimeRecord.getTuesdayMinute() != mCustomTimeRecord.getTuesdayMinute())
            mRemoteCustomTimeRecord.setTuesdayMinute(mCustomTimeRecord.getTuesdayMinute());

        if (mRemoteCustomTimeRecord.getWednesdayHour() != mCustomTimeRecord.getWednesdayHour())
            mRemoteCustomTimeRecord.setWednesdayHour(mCustomTimeRecord.getWednesdayHour());

        if (mRemoteCustomTimeRecord.getWednesdayMinute() != mCustomTimeRecord.getWednesdayMinute())
            mRemoteCustomTimeRecord.setWednesdayMinute(mCustomTimeRecord.getWednesdayMinute());

        if (mRemoteCustomTimeRecord.getThursdayHour() != mCustomTimeRecord.getThursdayHour())
            mRemoteCustomTimeRecord.setThursdayHour(mCustomTimeRecord.getThursdayHour());

        if (mRemoteCustomTimeRecord.getThursdayMinute() != mCustomTimeRecord.getThursdayMinute())
            mRemoteCustomTimeRecord.setThursdayMinute(mCustomTimeRecord.getThursdayMinute());

        if (mRemoteCustomTimeRecord.getFridayHour() != mCustomTimeRecord.getFridayHour())
            mRemoteCustomTimeRecord.setFridayHour(mCustomTimeRecord.getFridayHour());

        if (mRemoteCustomTimeRecord.getFridayMinute() != mCustomTimeRecord.getFridayMinute())
            mRemoteCustomTimeRecord.setFridayMinute(mCustomTimeRecord.getFridayMinute());

        if (mRemoteCustomTimeRecord.getSaturdayHour() != mCustomTimeRecord.getSaturdayHour())
            mRemoteCustomTimeRecord.setSaturdayHour(mCustomTimeRecord.getSaturdayHour());

        if (mRemoteCustomTimeRecord.getSaturdayMinute() != mCustomTimeRecord.getSaturdayMinute())
            mRemoteCustomTimeRecord.setSaturdayMinute(mCustomTimeRecord.getSaturdayMinute());
    }

    public void addRemoteCustomTimeRecord(@NonNull NewRemoteCustomTimeRecord remoteCustomTimeRecord) {
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

    public boolean hasRemoteRecord() {
        return (mRemoteCustomTimeRecord != null);
    }

    public boolean hasRemoteRecord(@NonNull String projectId) {
        Assert.assertTrue(!TextUtils.isEmpty(projectId));

        return mRemoteCustomTimeRecords.containsKey(projectId);
    }

    void clearRemoteRecords() {
        mRemoteCustomTimeRecord = null;

        mRemoteCustomTimeRecords.clear();
    }

    @NonNull
    public String getRemoteId() {
        Assert.assertTrue(mRemoteCustomTimeRecord != null);

        return mRemoteCustomTimeRecord.getId();
    }

    @NonNull
    public String getRemoteId(@NonNull String projectId) {
        Assert.assertTrue(!TextUtils.isEmpty(projectId));
        Assert.assertTrue(mRemoteCustomTimeRecords.containsKey(projectId));

        NewRemoteCustomTimeRecord remoteCustomTimeRecord = mRemoteCustomTimeRecords.get(projectId);
        Assert.assertTrue(remoteCustomTimeRecord != null);

        return remoteCustomTimeRecord.getId();
    }

    @Override
    public void updateRecordOf(@NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends) {// todo customtime remove
        Assert.assertTrue(mRemoteCustomTimeRecord != null);

        mRemoteCustomTimeRecord.updateRecordOf(addedFriends, removedFriends);
    }
}
