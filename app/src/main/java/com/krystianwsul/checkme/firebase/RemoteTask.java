package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

import java.util.List;

public class RemoteTask {
    @NonNull
    private final RemoteTaskRecord mRemoteTaskRecord;

    public RemoteTask(@NonNull RemoteTaskRecord remoteTaskRecord) {
        mRemoteTaskRecord = remoteTaskRecord;
    }

    @NonNull
    public String getName() {
        return mRemoteTaskRecord.getName();
    }

    @NonNull
    List<RemoteSchedule> getCurrentSchedules(@NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(current(exactTimeStamp));

        return Stream.of(getRemoteSchedules())
                .filter(schedule -> schedule.current(exactTimeStamp))
                .collect(Collectors.toList());
    }

    @NonNull
    private List<RemoteSchedule> getRemoteSchedules() {
        List<RemoteSchedule> remoteSchedules = Stream.of(mRemoteTaskRecord.getSingleScheduleRecords())
                .map(RemoteSingleSchedule::new)
                .collect(Collectors.toList());

        remoteSchedules.addAll(Stream.of(mRemoteTaskRecord.getDailyScheduleRecords())
                .map(RemoteDailySchedule::new)
                .collect(Collectors.toList()));

        remoteSchedules.addAll(Stream.of(mRemoteTaskRecord.getWeeklyScheduleRecords())
                .map(RemoteWeeklySchedule::new)
                .collect(Collectors.toList()));

        remoteSchedules.addAll(Stream.of(mRemoteTaskRecord.getMonthlyDayScheduleRecords())
                .map(RemoteMonthlyDaySchedule::new)
                .collect(Collectors.toList()));

        remoteSchedules.addAll(Stream.of(mRemoteTaskRecord.getMonthlyWeekScheduleRecords())
                .map(RemoteMonthlyWeekSchedule::new)
                .collect(Collectors.toList()));

        return remoteSchedules;
    }

    public boolean current(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    @NonNull
    public ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(mRemoteTaskRecord.getStartTime());
    }

    @Nullable
    ExactTimeStamp getEndExactTimeStamp() {
        if (mRemoteTaskRecord.getEndTime() != null)
            return new ExactTimeStamp(mRemoteTaskRecord.getEndTime());
        else
            return null;
    }

    @Nullable
    public String getScheduleText(@NonNull Context context, @NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(current(exactTimeStamp));

        List<RemoteSchedule> currentSchedules = getCurrentSchedules(exactTimeStamp);

        if (isRootTask(exactTimeStamp)) {
            if (currentSchedules.isEmpty())
                return null;

            Assert.assertTrue(Stream.of(currentSchedules)
                    .allMatch(schedule -> schedule.current(exactTimeStamp)));

            return Stream.of(currentSchedules)
                    .map(schedule -> schedule.getScheduleText(context))
                    .collect(Collectors.joining(", "));
        } else {
            Assert.assertTrue(currentSchedules.isEmpty());
            return null;
        }
    }

    boolean isRootTask(@NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(current(exactTimeStamp));

        return true; // todo firebase
    }

    @Nullable
    public String getNote() {
        return mRemoteTaskRecord.getNote();
    }
}
