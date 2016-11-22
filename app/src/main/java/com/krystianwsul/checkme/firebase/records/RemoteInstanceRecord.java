package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.firebase.json.InstanceJson;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.ScheduleKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

public class RemoteInstanceRecord extends RemoteRecord {
    @NonNull
    private final RemoteTaskRecord mRemoteTaskRecord;

    @NonNull
    private final InstanceJson mInstanceJson;

    RemoteInstanceRecord(boolean create, @NonNull RemoteTaskRecord remoteTaskRecord, @NonNull InstanceJson instanceJson) {
        super(create);

        mRemoteTaskRecord = remoteTaskRecord;
        mInstanceJson = instanceJson;
    }

    @NonNull
    @Override
    protected String getKey() {
        return mRemoteTaskRecord.getId() + "/" + RemoteTaskRecord.TASK_JSON + "/instances/" + scheduleKeyToString(getScheduleKey());
    }

    @NonNull
    private static ScheduleKey getScheduleKey(@NonNull RemoteInstanceRecord remoteInstanceRecord) {
        Date scheduleDate = new Date(remoteInstanceRecord.getScheduleYear(), remoteInstanceRecord.getScheduleMonth(), remoteInstanceRecord.getScheduleDay());

        String scheduleCustomTimeId = remoteInstanceRecord.getScheduleCustomTimeId();

        TimePair scheduleTimePair;
        if (!TextUtils.isEmpty(scheduleCustomTimeId)) {
            Assert.assertTrue(remoteInstanceRecord.getScheduleHour() == null);
            Assert.assertTrue(remoteInstanceRecord.getScheduleMinute() == null);

            scheduleTimePair = new TimePair(new CustomTimeKey(scheduleCustomTimeId));
        } else {
            Assert.assertTrue(remoteInstanceRecord.getScheduleHour() != null);
            Assert.assertTrue(remoteInstanceRecord.getScheduleMinute() != null);

            scheduleTimePair = new TimePair(new HourMinute(remoteInstanceRecord.getScheduleHour(), remoteInstanceRecord.getScheduleMinute()));
        }

        return new ScheduleKey(scheduleDate, scheduleTimePair);
    }

    @NonNull
    ScheduleKey getScheduleKey() {
        return getScheduleKey(this);
    }

    @NonNull
    public static String scheduleKeyToString(@NonNull ScheduleKey scheduleKey) {
        String key = scheduleKey.ScheduleDate.getYear() + "-" + scheduleKey.ScheduleDate.getMonth() + "-" + scheduleKey.ScheduleDate.getDay();
        if (scheduleKey.ScheduleTimePair.mCustomTimeKey != null) {
            Assert.assertTrue(scheduleKey.ScheduleTimePair.mHourMinute == null);
            Assert.assertTrue(!TextUtils.isEmpty(scheduleKey.ScheduleTimePair.mCustomTimeKey.mRemoteCustomTimeId));

            key += "-" + scheduleKey.ScheduleTimePair.mCustomTimeKey.mRemoteCustomTimeId;
        } else {
            Assert.assertTrue(scheduleKey.ScheduleTimePair.mHourMinute != null);

            key += "-" + scheduleKey.ScheduleTimePair.mHourMinute.getHour() + "-" + scheduleKey.ScheduleTimePair.mHourMinute.getMinute();
        }

        return key;
    }

    @NonNull
    @Override
    protected InstanceJson getCreateObject() {
        return mInstanceJson;
    }

    @NonNull
    public String getTaskId() {
        return mRemoteTaskRecord.getId();
    }

    public Long getDone() {
        return mInstanceJson.getDone();
    }

    public int getScheduleYear() {
        return mInstanceJson.getScheduleYear();
    }

    public int getScheduleMonth() {
        return mInstanceJson.getScheduleMonth();
    }

    public int getScheduleDay() {
        return mInstanceJson.getScheduleDay();
    }

    @Nullable
    public String getScheduleCustomTimeId() {
        return mInstanceJson.getScheduleCustomTimeId();
    }

    @Nullable
    public Integer getScheduleHour() {
        return mInstanceJson.getScheduleHour();
    }

    @Nullable
    public Integer getScheduleMinute() {
        return mInstanceJson.getScheduleMinute();
    }

    @Nullable
    public Integer getInstanceYear() {
        return mInstanceJson.getInstanceYear();
    }

    @Nullable
    public Integer getInstanceMonth() {
        return mInstanceJson.getInstanceMonth();
    }

    @Nullable
    public Integer getInstanceDay() {
        return mInstanceJson.getInstanceDay();
    }

    @Nullable
    public String getInstanceCustomTimeId() {
        return mInstanceJson.getInstanceCustomTimeId();
    }

    @Nullable
    public Integer getInstanceHour() {
        return mInstanceJson.getInstanceHour();
    }

    @Nullable
    public Integer getInstanceMinute() {
        return mInstanceJson.getInstanceMinute();
    }

    public long getHierarchyTime() {
        return mInstanceJson.getHierarchyTime();
    }

    public void setInstanceYear(int instanceYear) {
        if (getInstanceYear() != null && getInstanceYear().equals(instanceYear))
            return;

        mInstanceJson.setInstanceYear(instanceYear);
        addValue(getKey() + "/instanceYear", instanceYear);
    }

    public void setInstanceMonth(int instanceMonth) {
        if (getInstanceMonth() != null && getInstanceMonth().equals(instanceMonth))
            return;

        mInstanceJson.setInstanceMonth(instanceMonth);
        addValue(getKey() + "/instanceMonth", instanceMonth);
    }

    public void setInstanceDay(int instanceDay) {
        if (getInstanceDay() != null && getInstanceDay().equals(instanceDay))
            return;

        mInstanceJson.setInstanceDay(instanceDay);
        addValue(getKey() + "/instanceDay", instanceDay);
    }

    public void setInstanceCustomTimeId(@Nullable String instanceCustomTimeId) {
        if (!TextUtils.isEmpty(getInstanceCustomTimeId()) && getInstanceCustomTimeId().equals(instanceCustomTimeId))
            return;

        mInstanceJson.setInstanceCustomTimeId(instanceCustomTimeId);
        addValue(getKey() + "/instanceCustomTimeId", instanceCustomTimeId);
    }

    public void setInstanceHour(@Nullable Integer instanceHour) {
        if (getInstanceHour() != null && getInstanceHour().equals(instanceHour))
            return;

        mInstanceJson.setInstanceHour(instanceHour);
        addValue(getKey() + "/instanceHour", instanceHour);
    }

    public void setInstanceMinute(@Nullable Integer instanceMinute) {
        if (getInstanceMinute() != null && getInstanceMinute().equals(instanceMinute))
            return;

        mInstanceJson.setInstanceMinute(instanceMinute);
        addValue(getKey() + "/instanceMinute", instanceMinute);
    }

    public void setDone(@Nullable Long done) {
        if (getDone() != null && getDone().equals(done))
            return;

        mInstanceJson.setDone(done);
        addValue(getKey() + "/done", done);
    }
}
