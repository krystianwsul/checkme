package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class CreateTaskLoader extends DomainLoader<CreateTaskLoader.Data> {
    @Nullable
    private final TaskKey mTaskKey;

    @NonNull
    private final List<TaskKey> mExcludedTaskKeys;

    public CreateTaskLoader(@NonNull Context context, @Nullable TaskKey taskKey, @NonNull List<TaskKey> excludedTaskKeys) {
        super(context);

        mTaskKey = taskKey;
        mExcludedTaskKeys = excludedTaskKeys;
    }

    @Override
    String getName() {
        return "CreateTaskLoader, taskKey: " + mTaskKey + ", excludedTaskKeys: " + mExcludedTaskKeys;
    }

    @Override
    public Data loadDomain(@NonNull DomainFactory domainFactory) {
        return domainFactory.getCreateTaskData(mTaskKey, getContext(), mExcludedTaskKeys);
    }

    public interface ScheduleData {
        ScheduleType getScheduleType();
    }

    public static class Data extends DomainLoader.Data {
        @Nullable
        public final TaskData TaskData;

        @NonNull
        public final Map<TaskKey, TaskTreeData> TaskTreeDatas;

        @NonNull
        public final Map<CustomTimeKey, CustomTimeData> CustomTimeDatas;

        @NonNull
        public final Set<UserData> mFriends;

        public Data(@Nullable TaskData taskData, @NonNull Map<TaskKey, TaskTreeData> taskTreeDatas, @NonNull Map<CustomTimeKey, CustomTimeData> customTimeDatas, @NonNull Set<UserData> friends) {
            TaskData = taskData;
            TaskTreeDatas = taskTreeDatas;
            CustomTimeDatas = customTimeDatas;
            mFriends = friends;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            if (TaskData != null)
                hash += TaskData.hashCode();
            hash += TaskTreeDatas.hashCode();
            hash += CustomTimeDatas.hashCode();
            hash += mFriends.hashCode();
            return hash;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof Data))
                return false;

            Data data = (Data) object;

            if ((TaskData == null) != (data.TaskData == null))
                return false;

            if ((TaskData != null) && !TaskData.equals(data.TaskData))
                return false;

            if (!TaskTreeDatas.equals(data.TaskTreeDatas))
                return false;

            if (!CustomTimeDatas.equals(data.CustomTimeDatas))
                return false;

            if (!mFriends.equals(data.mFriends))
                return false;

            return true;
        }
    }

    public static class TaskData {
        @NonNull
        public final String Name;

        @Nullable
        public final TaskKey mParentTaskKey;

        @Nullable
        public final List<ScheduleData> ScheduleDatas;

        @Nullable
        public final String mNote;

        @NonNull
        public final Set<UserData> mFriends;

        public TaskData(@NonNull String name, @Nullable TaskKey parentTaskKey, @Nullable List<ScheduleData> scheduleDatas, @Nullable String note, @NonNull Set<UserData> friends) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue((parentTaskKey == null) || (scheduleDatas == null));

            Name = name;
            mParentTaskKey = parentTaskKey;
            ScheduleDatas = scheduleDatas;
            mNote = note;
            mFriends = friends;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            hash += Name.hashCode();
            if (mParentTaskKey != null) {
                Assert.assertTrue(ScheduleDatas == null);

                hash += mParentTaskKey.hashCode();
            } else {
                Assert.assertTrue(ScheduleDatas != null);

                hash += ScheduleDatas.hashCode();
            }
            if (!TextUtils.isEmpty(mNote))
                hash += mNote.hashCode();
            hash += mFriends.hashCode();
            return hash;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof TaskData))
                return false;

            TaskData taskData = (TaskData) object;

            if (!Name.equals(taskData.Name))
                return false;

            if ((mParentTaskKey == null) != (taskData.mParentTaskKey == null))
                return false;

            if ((mParentTaskKey != null) && !mParentTaskKey.equals(taskData.mParentTaskKey))
                return false;

            if ((ScheduleDatas == null) != (taskData.ScheduleDatas == null))
                return false;

            if ((ScheduleDatas != null) && !ScheduleDatas.equals(taskData.ScheduleDatas))
                return false;

            if (TextUtils.isEmpty(mNote) != TextUtils.isEmpty(taskData.mNote))
                return false;

            if (!TextUtils.isEmpty(mNote) && !mNote.equals(taskData.mNote))
                return false;

            if (!mFriends.equals(taskData.mFriends))
                return false;

            return true;
        }
    }

    public static class TaskTreeData {
        @NonNull
        public final String Name;

        @NonNull
        public final Map<TaskKey, TaskTreeData> TaskDatas;

        @NonNull
        public final TaskKey mTaskKey;

        public final String ScheduleText;

        @Nullable
        public final String mNote;

        @NonNull
        public final ExactTimeStamp mStartExactTimeStamp;

        public TaskTreeData(@NonNull String name, @NonNull Map<TaskKey, TaskTreeData> taskDatas, @NonNull TaskKey taskKey, @Nullable String scheduleText, @Nullable String note, @NonNull ExactTimeStamp startExactTimeStamp) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            Name = name;
            TaskDatas = taskDatas;
            mTaskKey = taskKey;
            ScheduleText = scheduleText;
            mNote = note;
            mStartExactTimeStamp = startExactTimeStamp;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            hash += Name.hashCode();
            hash += TaskDatas.hashCode();
            hash += mTaskKey.hashCode();
            if (!TextUtils.isEmpty(ScheduleText))
                hash += ScheduleText.hashCode();
            if (!TextUtils.isEmpty(mNote))
                hash += mNote.hashCode();
            return hash;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof TaskTreeData))
                return false;

            TaskTreeData taskTreeData = (TaskTreeData) object;

            if (!Name.equals(taskTreeData.Name))
                return false;

            if (!TaskDatas.equals(taskTreeData.TaskDatas))
                return false;

            if (!mTaskKey.equals(taskTreeData.mTaskKey))
                return false;

            if (TextUtils.isEmpty(ScheduleText) != TextUtils.isEmpty(taskTreeData.ScheduleText))
                return false;

            if (!TextUtils.isEmpty(ScheduleText) && !ScheduleText.equals(taskTreeData.ScheduleText))
                return false;

            if (TextUtils.isEmpty(mNote) != TextUtils.isEmpty(taskTreeData.mNote))
                return false;

            if (!TextUtils.isEmpty(mNote) && !mNote.equals(taskTreeData.mNote))
                return false;

            return true;
        }
    }

    public static class SingleScheduleData implements ScheduleData {
        @NonNull
        public final Date Date;

        @NonNull
        public final TimePair TimePair;

        public SingleScheduleData(@NonNull Date date, @NonNull TimePair timePair) {
            Date = date;
            TimePair = timePair;
        }

        @Override
        public int hashCode() {
            return (Date.hashCode() + TimePair.hashCode());
        }

        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof SingleScheduleData))
                return false;

            SingleScheduleData singleScheduleData = (SingleScheduleData) object;

            return (Date.equals(singleScheduleData.Date) && TimePair.equals(singleScheduleData.TimePair));
        }

        @Override
        public ScheduleType getScheduleType() {
            return ScheduleType.SINGLE;
        }
    }

    public static class CustomTimeData {
        @NonNull
        public final CustomTimeKey mCustomTimeKey;

        @NonNull
        public final String Name;

        @NonNull
        public final TreeMap<DayOfWeek, HourMinute> HourMinutes;

        public CustomTimeData(@NonNull CustomTimeKey customTimeKey, @NonNull String name, @NonNull TreeMap<DayOfWeek, HourMinute> hourMinutes) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(hourMinutes.size() == 7);

            mCustomTimeKey = customTimeKey;
            Name = name;
            HourMinutes = hourMinutes;
        }

        @Override
        public int hashCode() {
            return (mCustomTimeKey.hashCode() + Name.hashCode() + HourMinutes.hashCode());
        }

        @SuppressWarnings("SimplifiableIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof CustomTimeData))
                return false;

            CustomTimeData customTimeData = (CustomTimeData) object;

            if (!mCustomTimeKey.equals(customTimeData.mCustomTimeKey))
                return false;

            if (!Name.equals(customTimeData.Name))
                return false;

            return (HourMinutes.equals(customTimeData.HourMinutes));
        }
    }

    public static class DailyScheduleData implements ScheduleData {
        @NonNull
        public final TimePair TimePair;

        public DailyScheduleData(@NonNull TimePair timePair) {
            TimePair = timePair;
        }

        @Override
        public int hashCode() {
            return TimePair.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof DailyScheduleData))
                return false;

            DailyScheduleData dailyScheduleData = (DailyScheduleData) object;

            return TimePair.equals(dailyScheduleData.TimePair);
        }

        @Override
        public ScheduleType getScheduleType() {
            return ScheduleType.DAILY;
        }
    }

    public static class WeeklyScheduleData implements ScheduleData {
        @NonNull
        public final DayOfWeek DayOfWeek;

        @NonNull
        public final TimePair TimePair;

        public WeeklyScheduleData(@NonNull DayOfWeek dayOfWeek, @NonNull TimePair timePair) {
            DayOfWeek = dayOfWeek;
            TimePair = timePair;
        }

        @Override
        public int hashCode() {
            return (DayOfWeek.hashCode() + TimePair.hashCode());
        }

        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof WeeklyScheduleData))
                return false;

            WeeklyScheduleData weeklyScheduleData = (WeeklyScheduleData) object;

            return (DayOfWeek.equals(weeklyScheduleData.DayOfWeek) && TimePair.equals(weeklyScheduleData.TimePair));
        }

        @Override
        public ScheduleType getScheduleType() {
            return ScheduleType.WEEKLY;
        }
    }

    public static class MonthlyDayScheduleData implements ScheduleData {
        public final int mDayOfMonth;
        public final boolean mBeginningOfMonth;

        @NonNull
        public final TimePair TimePair;

        public MonthlyDayScheduleData(int dayOfMonth, boolean beginningOfMonth, @NonNull TimePair timePair) {
            mDayOfMonth = dayOfMonth;
            mBeginningOfMonth = beginningOfMonth;
            TimePair = timePair;
        }

        @Override
        public int hashCode() {
            int hashCode = mDayOfMonth;
            hashCode += (mBeginningOfMonth ? 1 : 0);
            hashCode += TimePair.hashCode();
            return hashCode;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof MonthlyDayScheduleData))
                return false;

            MonthlyDayScheduleData monthlyDayScheduleData = (MonthlyDayScheduleData) object;

            if (mDayOfMonth != monthlyDayScheduleData.mDayOfMonth)
                return false;

            if (mBeginningOfMonth != monthlyDayScheduleData.mBeginningOfMonth)
                return false;

            if (!TimePair.equals(monthlyDayScheduleData.TimePair))
                return false;

            return true;
        }

        @Override
        public ScheduleType getScheduleType() {
            return ScheduleType.MONTHLY_DAY;
        }
    }

    public static class MonthlyWeekScheduleData implements ScheduleData {
        public final int mDayOfMonth;

        @NonNull
        public final DayOfWeek mDayOfWeek;

        public final boolean mBeginningOfMonth;

        @NonNull
        public final TimePair TimePair;

        public MonthlyWeekScheduleData(int dayOfMonth, @NonNull DayOfWeek dayOfWeek, boolean beginningOfMonth, @NonNull TimePair timePair) {
            mDayOfMonth = dayOfMonth;
            mDayOfWeek = dayOfWeek;
            mBeginningOfMonth = beginningOfMonth;
            TimePair = timePair;
        }

        @Override
        public int hashCode() {
            int hashCode = mDayOfMonth;
            hashCode += mDayOfWeek.hashCode();
            hashCode += (mBeginningOfMonth ? 1 : 0);
            hashCode += TimePair.hashCode();
            return hashCode;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof MonthlyWeekScheduleData))
                return false;

            MonthlyWeekScheduleData monthlyWeekScheduleData = (MonthlyWeekScheduleData) object;

            if (mDayOfMonth != monthlyWeekScheduleData.mDayOfMonth)
                return false;

            if (mBeginningOfMonth != monthlyWeekScheduleData.mBeginningOfMonth)
                return false;

            if (!mDayOfWeek.equals(monthlyWeekScheduleData.mDayOfWeek))
                return false;

            if (!TimePair.equals(monthlyWeekScheduleData.TimePair))
                return false;

            return true;
        }

        @Override
        public ScheduleType getScheduleType() {
            return ScheduleType.MONTHLY_WEEK;
        }
    }
}
