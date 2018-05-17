package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.firebase.json.InstanceJson;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.ScheduleKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemoteInstanceRecord extends RemoteRecord {
    private static final Pattern sHourMinutePattern = Pattern.compile("^(\\d\\d\\d\\d)-(\\d?\\d)-(\\d?\\d)-(\\d?\\d)-(\\d?\\d)$");
    private static final Pattern sCustomTimePattern = Pattern.compile("^(\\d\\d\\d\\d)-(\\d?\\d)-(\\d?\\d)-(.+)$");

    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final RemoteTaskRecord mRemoteTaskRecord;

    @NonNull
    private final InstanceJson mInstanceJson;

    @NonNull
    private final ScheduleKey mScheduleKey;

    RemoteInstanceRecord(boolean create, @NonNull DomainFactory domainFactory, @NonNull RemoteTaskRecord remoteTaskRecord, @NonNull InstanceJson instanceJson, @NonNull ScheduleKey scheduleKey) {
        super(create);

        mDomainFactory = domainFactory;
        mRemoteTaskRecord = remoteTaskRecord;
        mInstanceJson = instanceJson;
        mScheduleKey = scheduleKey;
    }

    @NonNull
    @Override
    protected String getKey() {
        return mRemoteTaskRecord.getKey() + "/instances/" + scheduleKeyToString(mDomainFactory, mRemoteTaskRecord.getProjectId(), getScheduleKey());
    }

    @NonNull
    ScheduleKey getScheduleKey() {
        return mScheduleKey;
    }

    @NonNull
    public static String scheduleKeyToString(@NonNull DomainFactory domainFactory, @NonNull String projectId, @NonNull ScheduleKey scheduleKey) {
        String key = scheduleKey.getScheduleDate().getYear() + "-" + scheduleKey.getScheduleDate().getMonth() + "-" + scheduleKey.getScheduleDate().getDay();
        if (scheduleKey.getScheduleTimePair().getCustomTimeKey() != null) {
            Assert.assertTrue(scheduleKey.getScheduleTimePair().getHourMinute() == null);

            key += "-" + domainFactory.getRemoteCustomTimeId(projectId, scheduleKey.getScheduleTimePair().getCustomTimeKey());
        } else {
            Assert.assertTrue(scheduleKey.getScheduleTimePair().getHourMinute() != null);

            key += "-" + scheduleKey.getScheduleTimePair().getHourMinute().getHour() + "-" + scheduleKey.getScheduleTimePair().getHourMinute().getMinute();
        }

        return key;
    }

    @NonNull
    static ScheduleKey stringToScheduleKey(@NonNull DomainFactory domainFactory, @NonNull String projectId, @NonNull String key) {
        Matcher hourMinuteMatcher = sHourMinutePattern.matcher(key);

        if (hourMinuteMatcher.matches()) {
            int year = Integer.valueOf(hourMinuteMatcher.group(1));
            int month = Integer.valueOf(hourMinuteMatcher.group(2));
            int day = Integer.valueOf(hourMinuteMatcher.group(3));
            int hour = Integer.valueOf(hourMinuteMatcher.group(4));
            int minute = Integer.valueOf(hourMinuteMatcher.group(5));

            return new ScheduleKey(new Date(year, month, day), new TimePair(new HourMinute(hour, minute)));
        } else {
            Matcher customTimeMatcher = sCustomTimePattern.matcher(key);
            Assert.assertTrue(customTimeMatcher.matches());

            int year = Integer.valueOf(customTimeMatcher.group(1));
            int month = Integer.valueOf(customTimeMatcher.group(2));
            int day = Integer.valueOf(customTimeMatcher.group(3));

            String customTimeId = customTimeMatcher.group(4);
            Assert.assertTrue(!TextUtils.isEmpty(customTimeId));

            CustomTimeKey customTimeKey = domainFactory.getCustomTimeKey(projectId, customTimeId);

            return new ScheduleKey(new Date(year, month, day), new TimePair(customTimeKey));
        }
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
        return mScheduleKey.getScheduleDate().getYear();
    }

    public int getScheduleMonth() {
        return mScheduleKey.getScheduleDate().getMonth();
    }

    public int getScheduleDay() {
        return mScheduleKey.getScheduleDate().getDay();
    }

    @Nullable
    public String getScheduleCustomTimeId() {
        CustomTimeKey customTimeKey = mScheduleKey.getScheduleTimePair().getCustomTimeKey();
        if (customTimeKey != null) {
            return mDomainFactory.getRemoteCustomTimeId(mRemoteTaskRecord.getProjectId(), customTimeKey);
        } else {
            return null;
        }
    }

    @Nullable
    public Integer getScheduleHour() {
        HourMinute hourMinute = mScheduleKey.getScheduleTimePair().getHourMinute();
        if (hourMinute != null) {
            return hourMinute.getHour();
        } else {
            return null;
        }
    }

    @Nullable
    public Integer getScheduleMinute() {
        HourMinute hourMinute = mScheduleKey.getScheduleTimePair().getHourMinute();
        if (hourMinute != null) {
            return hourMinute.getMinute();
        } else {
            return null;
        }
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

    @Nullable
    public Double getOrdinal() {
        return mInstanceJson.getOrdinal();
    }

    public void setOrdinal(double ordinal) {
        if (getOrdinal() != null && getOrdinal().equals(ordinal))
            return;

        mInstanceJson.setOrdinal(ordinal);
        addValue(getKey() + "/ordinal", ordinal);
    }
}
