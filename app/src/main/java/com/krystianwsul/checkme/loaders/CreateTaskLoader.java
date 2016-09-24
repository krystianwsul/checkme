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
    private final Integer mTaskId;
    private final List<Integer> mExcludedTaskIds;

    public CreateTaskLoader(Context context, Integer taskId, List<Integer> excludedTaskIds) {
        super(context);

        Assert.assertTrue(excludedTaskIds != null);

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

        public TaskData(@NonNull String name, Integer parentTaskId, List<ScheduleData> scheduleDatas, @Nullable String note) {
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

        public TaskTreeData(String name, TreeMap<Integer, TaskTreeData> taskDatas, int taskId, String scheduleText) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(taskDatas != null);

            Name = name;
            TaskDatas = taskDatas;
            TaskId = taskId;
            ScheduleText = scheduleText;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            hash += Name.hashCode();
            hash += TaskDatas.hashCode();
            hash += TaskId;
            if (!TextUtils.isEmpty(ScheduleText))
                hash += ScheduleText.hashCode();
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

            return true;
        }
    }

    public static class SingleScheduleData implements ScheduleData {
        public final com.krystianwsul.checkme.utils.time.Date Date;
        public final com.krystianwsul.checkme.utils.time.TimePair TimePair;

        public SingleScheduleData(Date date, TimePair timePair) {
            Assert.assertTrue(date != null);
            Assert.assertTrue(timePair != null);

            Date = date;
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
        public final TimePair TimePair;

        public DailyScheduleData(TimePair timePair) {
            Assert.assertTrue(timePair != null);
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
        public final DayOfWeek DayOfWeek;
        public final TimePair TimePair;

        public WeeklyScheduleData(DayOfWeek dayOfWeek, TimePair timePair) {
            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(timePair != null);

            DayOfWeek = dayOfWeek;
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
}
