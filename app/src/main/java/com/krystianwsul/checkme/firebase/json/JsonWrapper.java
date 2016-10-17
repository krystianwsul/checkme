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

    public JsonWrapper() {

    }

    public JsonWrapper(@NonNull List<UserData> userDatas, @NonNull TaskJson taskJson) {
        Assert.assertTrue(!userDatas.isEmpty());

        recordOf = Stream.of(userDatas)
                .collect(Collectors.toMap(friend -> UserData.getKey(friend.email), friend -> true));

        this.taskJson = taskJson;
        taskHierarchyJson = null;
        singleScheduleJson = null;
        dailyScheduleJson = null;
        weeklyScheduleJson = null;
        monthlyDayScheduleJson = null;
        monthlyWeekScheduleJson = null;
    }

    public JsonWrapper(@NonNull List<UserData> userDatas, @NonNull TaskHierarchyJson taskHierarchyJson) {
        Assert.assertTrue(!userDatas.isEmpty());

        recordOf = Stream.of(userDatas)
                .collect(Collectors.toMap(friend -> UserData.getKey(friend.email), friend -> true));

        taskJson = null;
        this.taskHierarchyJson = taskHierarchyJson;
        singleScheduleJson = null;
        dailyScheduleJson = null;
        weeklyScheduleJson = null;
        monthlyDayScheduleJson = null;
        monthlyWeekScheduleJson = null;
    }

    public JsonWrapper(@NonNull Set<String> recordOf, @NonNull TaskJson taskJson) {
        Assert.assertTrue(!recordOf.isEmpty());

        this.recordOf = Stream.of(recordOf)
                .collect(Collectors.toMap(friend -> friend, friend -> true));

        this.taskJson = taskJson;
        taskHierarchyJson = null;
        singleScheduleJson = null;
        dailyScheduleJson = null;
        weeklyScheduleJson = null;
        monthlyDayScheduleJson = null;
        monthlyWeekScheduleJson = null;
    }

    public JsonWrapper(@NonNull Set<String> recordOf, @NonNull TaskHierarchyJson taskHierarchyJson) {
        Assert.assertTrue(!recordOf.isEmpty());

        this.recordOf = Stream.of(recordOf)
                .collect(Collectors.toMap(friend -> friend, friend -> true));

        taskJson = null;
        this.taskHierarchyJson = taskHierarchyJson;
        singleScheduleJson = null;
        dailyScheduleJson = null;
        weeklyScheduleJson = null;
        monthlyDayScheduleJson = null;
        monthlyWeekScheduleJson = null;
    }

    public JsonWrapper(@NonNull List<UserData> userDatas, @NonNull SingleScheduleJson singleScheduleJson) {
        Assert.assertTrue(!userDatas.isEmpty());

        recordOf = Stream.of(userDatas)
                .collect(Collectors.toMap(friend -> UserData.getKey(friend.email), friend -> true));

        taskJson = null;
        taskHierarchyJson = null;
        this.singleScheduleJson = singleScheduleJson;
        dailyScheduleJson = null;
        weeklyScheduleJson = null;
        monthlyDayScheduleJson = null;
        monthlyWeekScheduleJson = null;
    }

    public JsonWrapper(@NonNull List<UserData> userDatas, @NonNull DailyScheduleJson dailyScheduleJson) {
        Assert.assertTrue(!userDatas.isEmpty());

        recordOf = Stream.of(userDatas)
                .collect(Collectors.toMap(friend -> UserData.getKey(friend.email), friend -> true));

        taskJson = null;
        taskHierarchyJson = null;
        singleScheduleJson = null;
        this.dailyScheduleJson = dailyScheduleJson;
        weeklyScheduleJson = null;
        monthlyDayScheduleJson = null;
        monthlyWeekScheduleJson = null;
    }

    public JsonWrapper(@NonNull List<UserData> userDatas, @NonNull WeeklyScheduleJson weeklyScheduleJson) {
        Assert.assertTrue(!userDatas.isEmpty());

        recordOf = Stream.of(userDatas)
                .collect(Collectors.toMap(friend -> UserData.getKey(friend.email), friend -> true));

        taskJson = null;
        taskHierarchyJson = null;
        singleScheduleJson = null;
        dailyScheduleJson = null;
        this.weeklyScheduleJson = weeklyScheduleJson;
        monthlyDayScheduleJson = null;
        monthlyWeekScheduleJson = null;
    }

    public JsonWrapper(@NonNull List<UserData> userDatas, @NonNull MonthlyDayScheduleJson monthlyDayScheduleJson) {
        Assert.assertTrue(!userDatas.isEmpty());

        recordOf = Stream.of(userDatas)
                .collect(Collectors.toMap(friend -> UserData.getKey(friend.email), friend -> true));

        taskJson = null;
        taskHierarchyJson = null;
        singleScheduleJson = null;
        dailyScheduleJson = null;
        weeklyScheduleJson = null;
        this.monthlyDayScheduleJson = monthlyDayScheduleJson;
        monthlyWeekScheduleJson = null;
    }

    public JsonWrapper(@NonNull List<UserData> userDatas, @NonNull MonthlyWeekScheduleJson monthlyWeekScheduleJson) {
        Assert.assertTrue(!userDatas.isEmpty());

        recordOf = Stream.of(userDatas)
                .collect(Collectors.toMap(friend -> UserData.getKey(friend.email), friend -> true));

        taskJson = null;
        taskHierarchyJson = null;
        singleScheduleJson = null;
        dailyScheduleJson = null;
        weeklyScheduleJson = null;
        monthlyDayScheduleJson = null;
        this.monthlyWeekScheduleJson = monthlyWeekScheduleJson;
    }
}
