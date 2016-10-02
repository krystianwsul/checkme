package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CreateTaskLoader extends DomainLoader<CreateTaskLoader.Data> {
    @Nullable
    private final Integer mTaskId;

    @NonNull
    private final List<Integer> mExcludedTaskIds;

    public CreateTaskLoader(@NonNull Context context, @Nullable Integer taskId, @NonNull List<Integer> excludedTaskIds) {
        super(context);

        mTaskId = taskId;
        mExcludedTaskIds = excludedTaskIds;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getCreateChildTaskData(mTaskId, getContext(), mExcludedTaskIds);
    }

    public interface ScheduleData {
        ScheduleType getScheduleType();
    }

    public static class Data extends DomainLoader.Data {
        public final TaskData TaskData;
        public final TreeMap<Integer, TaskTreeData> TaskTreeDatas;
        public final Map<Integer, CustomTimeData> CustomTimeDatas;

        public Data(TaskData taskData, @NonNull TreeMap<Integer, TaskTreeData> taskTreeDatas, @NonNull Map<Integer, CustomTimeData> customTimeDatas) {
            TaskData = taskData;
            TaskTreeDatas = taskTreeDatas;
            CustomTimeDatas = customTimeDatas;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            if (TaskData != null)
                hash += TaskData.hashCode();
            hash += TaskTreeDatas.hashCode();
            hash += CustomTimeDatas.hashCode();
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

            return true;
        }
    }

    public static class TaskData {
        public final String Name;
        public final Integer ParentTaskId;
        public final List<ScheduleData> ScheduleDatas;
        public final String mNote;

        public TaskData(@NonNull String name, @Nullable Integer parentTaskId, @Nullable List<ScheduleData> scheduleDatas, @Nullable String note) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue((parentTaskId == null) || (scheduleDatas == null));

            Name = name;
            ParentTaskId = parentTaskId;
            ScheduleDatas = scheduleDatas;
            mNote = note;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            hash += Name.hashCode();
            if (ParentTaskId != null) {
                Assert.assertTrue(ScheduleDatas == null);

                hash += ParentTaskId;
            } else {
                Assert.assertTrue(ScheduleDatas != null);

                hash += ScheduleDatas.hashCode();
            }
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

            if (!(object instanceof TaskData))
                return false;

            TaskData taskData = (TaskData) object;

            if (!Name.equals(taskData.Name))
                return false;

            if ((ParentTaskId == null) != (taskData.ParentTaskId == null))
                return false;

            if ((ParentTaskId != null) && !ParentTaskId.equals(taskData.ParentTaskId))
                return false;

            if ((ScheduleDatas == null) != (taskData.ScheduleDatas == null))
                return false;

            if ((ScheduleDatas != null) && !ScheduleDatas.equals(taskData.ScheduleDatas))
                return false;

            if (TextUtils.isEmpty(mNote) != TextUtils.isEmpty(taskData.mNote))
                return false;

            if (!TextUtils.isEmpty(mNote) && !mNote.equals(taskData.mNote))
                return false;

            return true;
        }
    }

    public static class TaskTreeData {
        public final String Name;
        public final TreeMap<Integer, TaskTreeData> TaskDatas;
        public final int TaskId;
        public final String ScheduleText;
        public final String mNote;

        public TaskTreeData(@NonNull String name, @NonNull TreeMap<Integer, TaskTreeData> taskDatas, int taskId, @Nullable String scheduleText, @Nullable String note) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            Name = name;
            TaskDatas = taskDatas;
            TaskId = taskId;
            ScheduleText = scheduleText;
            mNote = note;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            hash += Name.hashCode();
            hash += TaskDatas.hashCode();
            hash += TaskId;
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

            if (TaskId != taskTreeData.TaskId)
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
        public final int Id;
        public final String Name;
        public final TreeMap<DayOfWeek, HourMinute> HourMinutes;

        public CustomTimeData(int id, String name, TreeMap<DayOfWeek, HourMinute> hourMinutes) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(hourMinutes != null);
            Assert.assertTrue(hourMinutes.size() == 7);

            Id = id;
            Name = name;
            HourMinutes = hourMinutes;
        }

        @Override
        public int hashCode() {
            return (Id + Name.hashCode() + HourMinutes.hashCode());
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

            if (Id != customTimeData.Id)
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
