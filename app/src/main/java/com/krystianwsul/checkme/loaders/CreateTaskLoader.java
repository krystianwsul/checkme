package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
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
import java.util.TreeMap;

public class CreateTaskLoader extends DomainLoader<CreateTaskLoader.Data> {
    @Nullable
    private final TaskKey mTaskKey;

    @Nullable
    private final List<TaskKey> mJoinTaskKeys;

    public CreateTaskLoader(@NonNull Context context, @Nullable TaskKey taskKey, @Nullable List<TaskKey> joinTaskKeys) {
        super(context, needsFirebase(taskKey));

        mTaskKey = taskKey;
        mJoinTaskKeys = joinTaskKeys;
    }

    @Override
    String getName() {
        return "CreateTaskLoader, taskKey: " + mTaskKey + ", excludedTaskKeys: " + mJoinTaskKeys;
    }

    @NonNull
    private static FirebaseLevel needsFirebase(@Nullable TaskKey taskKey) {
        if (taskKey != null && taskKey.getType() == TaskKey.Type.REMOTE) {
            return FirebaseLevel.NEED;
        } else {
            return FirebaseLevel.WANT;
        }
    }

    @Override
    public Data loadDomain(@NonNull DomainFactory domainFactory) {
        return domainFactory.getCreateTaskData(mTaskKey, getContext(), mJoinTaskKeys);
    }

    public interface ScheduleData {
        ScheduleType getScheduleType();
    }

    public static class Data extends DomainLoader.Data {
        @Nullable
        public final TaskData TaskData;

        @NonNull
        public final Map<ParentKey, ParentTreeData> mParentTreeDatas;

        @NonNull
        public final Map<CustomTimeKey, CustomTimeData> CustomTimeDatas;

        public Data(@Nullable TaskData taskData, @NonNull Map<ParentKey, ParentTreeData> parentTreeDatas, @NonNull Map<CustomTimeKey, CustomTimeData> customTimeDatas) {
            TaskData = taskData;
            mParentTreeDatas = parentTreeDatas;
            CustomTimeDatas = customTimeDatas;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            if (TaskData != null)
                hash += TaskData.hashCode();
            hash += mParentTreeDatas.hashCode();
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

            if (!mParentTreeDatas.equals(data.mParentTreeDatas))
                return false;

            if (!CustomTimeDatas.equals(data.CustomTimeDatas))
                return false;

            return true;
        }
    }

    public static class TaskData {
        @NonNull
        public final String Name;

        @Nullable
        public final ParentKey mParentKey;

        @Nullable
        public final List<ScheduleData> ScheduleDatas;

        @Nullable
        public final String mNote;

        public TaskData(@NonNull String name, @Nullable ParentKey parentKey, @Nullable List<ScheduleData> scheduleDatas, @Nullable String note) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            Name = name;
            mParentKey = parentKey;
            ScheduleDatas = scheduleDatas;
            mNote = note;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            hash += Name.hashCode();
            if (mParentKey != null) {
                Assert.assertTrue(ScheduleDatas == null);

                hash += mParentKey.hashCode();
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

            if ((mParentKey == null) != (taskData.mParentKey == null))
                return false;

            if ((mParentKey != null) && !mParentKey.equals(taskData.mParentKey))
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

    public static class ParentTreeData {
        @NonNull
        public final String Name;

        @NonNull
        public final Map<ParentKey, ParentTreeData> mParentTreeDatas;

        @NonNull
        public final ParentKey mParentKey;

        public final String ScheduleText;

        @Nullable
        public final String mNote;

        @NonNull
        public final SortKey mSortKey;

        public ParentTreeData(@NonNull String name, @NonNull Map<ParentKey, ParentTreeData> parentTreeDatas, @NonNull ParentKey parentKey, @Nullable String scheduleText, @Nullable String note, @NonNull SortKey sortKey) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            Name = name;
            mParentTreeDatas = parentTreeDatas;
            mParentKey = parentKey;
            ScheduleText = scheduleText;
            mNote = note;
            mSortKey = sortKey;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            hash += Name.hashCode();
            hash += mParentTreeDatas.hashCode();
            hash += mParentKey.hashCode();
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

            if (!(object instanceof ParentTreeData))
                return false;

            ParentTreeData parentTreeData = (ParentTreeData) object;

            if (!Name.equals(parentTreeData.Name))
                return false;

            if (!mParentTreeDatas.equals(parentTreeData.mParentTreeDatas))
                return false;

            if (!mParentKey.equals(parentTreeData.mParentKey))
                return false;

            if (TextUtils.isEmpty(ScheduleText) != TextUtils.isEmpty(parentTreeData.ScheduleText))
                return false;

            if (!TextUtils.isEmpty(ScheduleText) && !ScheduleText.equals(parentTreeData.ScheduleText))
                return false;

            if (TextUtils.isEmpty(mNote) != TextUtils.isEmpty(parentTreeData.mNote))
                return false;

            if (!TextUtils.isEmpty(mNote) && !mNote.equals(parentTreeData.mNote))
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

    public interface ParentKey extends Parcelable {
        @NonNull
        ParentType getType();
    }

    public enum ParentType {
        PROJECT, TASK
    }

    public static class ProjectParentKey implements ParentKey {
        @NonNull
        public final String mProjectId;

        public ProjectParentKey(@NonNull String projectId) {
            Assert.assertTrue(!TextUtils.isEmpty(projectId));

            mProjectId = projectId;
        }

        @NonNull
        @Override
        public ParentType getType() {
            return ParentType.PROJECT;
        }

        @Override
        public int hashCode() {
            return mProjectId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;

            if (obj == this)
                return true;

            if (!(obj instanceof ProjectParentKey))
                return false;

            ProjectParentKey projectParentKey = (ProjectParentKey) obj;

            return mProjectId.equals(projectParentKey.mProjectId);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mProjectId);
        }

        public static final Parcelable.Creator<ProjectParentKey> CREATOR = new Creator<ProjectParentKey>() {
            @Override
            public ProjectParentKey createFromParcel(Parcel in) {
                String projectId = in.readString();
                Assert.assertTrue(!TextUtils.isEmpty(projectId));

                return new ProjectParentKey(projectId);
            }

            @Override
            public ProjectParentKey[] newArray(int size) {
                return new ProjectParentKey[size];
            }
        };
    }

    public static class TaskParentKey implements ParentKey {
        @NonNull
        public final TaskKey mTaskKey;

        public TaskParentKey(@NonNull TaskKey taskKey) {
            mTaskKey = taskKey;
        }

        @NonNull
        @Override
        public ParentType getType() {
            return ParentType.TASK;
        }

        @Override
        public int hashCode() {
            return mTaskKey.hashCode();
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;

            if (obj == this)
                return true;

            if (!(obj instanceof TaskParentKey))
                return false;

            TaskParentKey taskParentKey = (TaskParentKey) obj;

            if (!mTaskKey.equals(taskParentKey.mTaskKey))
                return false;

            return true;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(mTaskKey, 0);
        }

        public static final Parcelable.Creator<TaskParentKey> CREATOR = new Creator<TaskParentKey>() {
            @Override
            public TaskParentKey createFromParcel(Parcel in) {
                TaskKey taskKey = in.readParcelable(TaskKey.class.getClassLoader());
                Assert.assertTrue(taskKey != null);

                return new TaskParentKey(taskKey);
            }

            @Override
            public TaskParentKey[] newArray(int size) {
                return new TaskParentKey[size];
            }
        };
    }

    @SuppressWarnings("WeakerAccess")
    public interface SortKey extends Comparable<SortKey> {

    }

    public static class ProjectSortKey implements SortKey {
        @NonNull
        private final String mProjectId;

        public ProjectSortKey(@NonNull String projectId) {
            Assert.assertTrue(!TextUtils.isEmpty(projectId));

            mProjectId = projectId;
        }

        @Override
        public int hashCode() {
            return mProjectId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;

            if (obj == this)
                return true;

            if (!(obj instanceof ProjectSortKey))
                return false;

            ProjectSortKey projectSortKey = (ProjectSortKey) obj;

            return mProjectId.equals(projectSortKey.mProjectId);
        }

        @Override
        public int compareTo(@NonNull SortKey sortKey) {
            if (sortKey instanceof TaskSortKey)
                return 1;

            Assert.assertTrue(sortKey instanceof ProjectSortKey);

            ProjectSortKey projectSortKey = (ProjectSortKey) sortKey;

            return mProjectId.compareTo(projectSortKey.mProjectId);
        }
    }

    public static class TaskSortKey implements SortKey {
        @NonNull
        private final ExactTimeStamp mStartExactTimeStamp;

        public TaskSortKey(@NonNull ExactTimeStamp startExactTimeStamp) {
            mStartExactTimeStamp = startExactTimeStamp;
        }

        @Override
        public int hashCode() {
            return mStartExactTimeStamp.hashCode();
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;

            if (obj == this)
                return true;

            if (!(obj instanceof TaskSortKey))
                return false;

            TaskSortKey taskSortKey = (TaskSortKey) obj;

            if (!mStartExactTimeStamp.equals(taskSortKey.mStartExactTimeStamp))
                return false;

            return true;
        }

        @Override
        public int compareTo(@NonNull SortKey sortKey) {
            if (sortKey instanceof ProjectSortKey)
                return -1;

            Assert.assertTrue(sortKey instanceof TaskSortKey);

            TaskSortKey taskSortKey = (TaskSortKey) sortKey;

            return taskSortKey.mStartExactTimeStamp.compareTo(taskSortKey.mStartExactTimeStamp);
        }
    }
}
