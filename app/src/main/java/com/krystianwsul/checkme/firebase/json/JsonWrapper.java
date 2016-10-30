package com.krystianwsul.checkme.firebase.json;

import android.support.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.firebase.database.IgnoreExtraProperties;
import com.krystianwsul.checkme.firebase.UserData;

import junit.framework.Assert;

import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unused", "WeakerAccess"})
@IgnoreExtraProperties
public class JsonWrapper {
    public Map<String, Boolean> recordOf;
    public TaskJson taskJson;
    public TaskHierarchyJson taskHierarchyJson;
    public SingleScheduleJson singleScheduleJson;
    public DailyScheduleJson dailyScheduleJson;
    public WeeklyScheduleJson weeklyScheduleJson;
    public MonthlyDayScheduleJson monthlyDayScheduleJson;
    public MonthlyWeekScheduleJson monthlyWeekScheduleJson;
    public InstanceJson instanceJson;
    public CustomTimeJson customTimeJson;

    public JsonWrapper() {

    }

    public JsonWrapper(@NonNull List<UserData> userDatas, @NonNull TaskHierarchyJson taskHierarchyJson) {
        Assert.assertTrue(!userDatas.isEmpty());

        recordOf = Stream.of(userDatas)
                .collect(Collectors.toMap(friend -> UserData.getKey(friend.email), friend -> true));

        this.taskHierarchyJson = taskHierarchyJson;
    }

    public JsonWrapper(@NonNull Set<String> recordOf, @NonNull TaskJson taskJson) {
        Assert.assertTrue(!recordOf.isEmpty());

        this.recordOf = Stream.of(recordOf)
                .collect(Collectors.toMap(friend -> friend, friend -> true));

        this.taskJson = taskJson;
    }

    public JsonWrapper(@NonNull Set<String> recordOf, @NonNull TaskHierarchyJson taskHierarchyJson) {
        Assert.assertTrue(!recordOf.isEmpty());

        this.recordOf = Stream.of(recordOf)
                .collect(Collectors.toMap(friend -> friend, friend -> true));

        this.taskHierarchyJson = taskHierarchyJson;
    }

    public JsonWrapper(@NonNull Set<String> recordOf, @NonNull SingleScheduleJson singleScheduleJson) {
        Assert.assertTrue(!recordOf.isEmpty());

        this.recordOf = Stream.of(recordOf)
                .collect(Collectors.toMap(friend -> friend, friend -> true));

        this.singleScheduleJson = singleScheduleJson;
    }

    public JsonWrapper(@NonNull Set<String> recordOf, @NonNull DailyScheduleJson dailyScheduleJson) {
        Assert.assertTrue(!recordOf.isEmpty());

        this.recordOf = Stream.of(recordOf)
                .collect(Collectors.toMap(friend -> friend, friend -> true));

        this.dailyScheduleJson = dailyScheduleJson;
    }

    public JsonWrapper(@NonNull Set<String> recordOf, @NonNull WeeklyScheduleJson weeklyScheduleJson) {
        Assert.assertTrue(!recordOf.isEmpty());

        this.recordOf = Stream.of(recordOf)
                .collect(Collectors.toMap(friend -> friend, friend -> true));

        this.weeklyScheduleJson = weeklyScheduleJson;
    }

    public JsonWrapper(@NonNull Set<String> recordOf, @NonNull MonthlyDayScheduleJson monthlyDayScheduleJson) {
        Assert.assertTrue(!recordOf.isEmpty());

        this.recordOf = Stream.of(recordOf)
                .collect(Collectors.toMap(friend -> friend, friend -> true));

        this.monthlyDayScheduleJson = monthlyDayScheduleJson;
    }

    public JsonWrapper(@NonNull Set<String> recordOf, @NonNull MonthlyWeekScheduleJson monthlyWeekScheduleJson) {
        Assert.assertTrue(!recordOf.isEmpty());

        this.recordOf = Stream.of(recordOf)
                .collect(Collectors.toMap(friend -> friend, friend -> true));

        this.monthlyWeekScheduleJson = monthlyWeekScheduleJson;
    }

    public JsonWrapper(@NonNull Set<String> recordOf, @NonNull InstanceJson instanceJson) {
        Assert.assertTrue(!recordOf.isEmpty());

        this.recordOf = Stream.of(recordOf)
                .collect(Collectors.toMap(friend -> friend, friend -> true));

        this.instanceJson = instanceJson;
    }

    public JsonWrapper(@NonNull Set<String> recordOf, @NonNull CustomTimeJson customTimeJsonJson) {
        Assert.assertTrue(!recordOf.isEmpty());

        this.recordOf = Stream.of(recordOf)
                .collect(Collectors.toMap(friend -> friend, friend -> true));

        this.customTimeJson = customTimeJsonJson;
    }

    public void setRecordOf(@NonNull Map<String, Boolean> recordOf) {
        this.recordOf = recordOf;
    }
}
