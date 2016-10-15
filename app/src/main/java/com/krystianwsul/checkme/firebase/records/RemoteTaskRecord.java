package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.firebase.json.DailyScheduleJson;
import com.krystianwsul.checkme.firebase.json.JsonWrapper;
import com.krystianwsul.checkme.firebase.json.MonthlyDayScheduleJson;
import com.krystianwsul.checkme.firebase.json.MonthlyWeekScheduleJson;
import com.krystianwsul.checkme.firebase.json.SingleScheduleJson;
import com.krystianwsul.checkme.firebase.json.TaskJson;
import com.krystianwsul.checkme.firebase.json.WeeklyScheduleJson;

import junit.framework.Assert;

import java.util.List;

public class RemoteTaskRecord extends RemoteRecord {
    public RemoteTaskRecord(@NonNull String id, @NonNull JsonWrapper jsonWrapper) {
        super(id, jsonWrapper);
    }

    public RemoteTaskRecord(@NonNull JsonWrapper jsonWrapper) {
        super(jsonWrapper);
    }

    @NonNull
    private TaskJson getTaskJson() {
        TaskJson taskJson = mJsonWrapper.taskJson;
        Assert.assertTrue(taskJson != null);

        return taskJson;
    }

    @NonNull
    public String getName() {
        return getTaskJson().getName();
    }

    @NonNull
    public List<SingleScheduleJson> getSingleScheduleRecords() {
        return getTaskJson().getSingleScheduleRecords();
    }

    @NonNull
    public List<DailyScheduleJson> getDailyScheduleRecords() {
        return getTaskJson().getDailyScheduleRecords();
    }

    @NonNull
    public List<WeeklyScheduleJson> getWeeklyScheduleRecords() {
        return getTaskJson().getWeeklyScheduleRecords();
    }

    @NonNull
    public List<MonthlyDayScheduleJson> getMonthlyDayScheduleRecords() {
        return getTaskJson().getMonthlyDayScheduleRecords();
    }

    @NonNull
    public List<MonthlyWeekScheduleJson> getMonthlyWeekScheduleRecords() {
        return getTaskJson().getMonthlyWeekScheduleRecords();
    }

    public long getStartTime() {
        return getTaskJson().getStartTime();
    }

    @Nullable
    public Long getEndTime() {
        return getTaskJson().getEndTime();
    }

    @Nullable
    public String getNote() {
        return getTaskJson().getNote();
    }
}
