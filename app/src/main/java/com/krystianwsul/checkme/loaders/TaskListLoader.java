package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

import java.util.List;

public class TaskListLoader extends DomainLoader<TaskListLoader.Data> {
    @Nullable
    private final TaskKey mTaskKey;

    public TaskListLoader(@NonNull Context context, @Nullable TaskKey taskKey) {
        super(context);
        mTaskKey = taskKey;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getTaskListData(getContext(), mTaskKey);
    }

    public static class Data extends DomainLoader.Data {
        public final List<ChildTaskData> mChildTaskDatas;
        public final String mNote;

        public Data(@NonNull List<ChildTaskData> childTaskDatas, @Nullable String note) {
            mChildTaskDatas = childTaskDatas;
            mNote = note;
        }

        @Override
        public int hashCode() {
            int hashCode = mChildTaskDatas.hashCode();
            if (!TextUtils.isEmpty(mNote))
                hashCode += mNote.hashCode();
            return hashCode;
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

            if (!mChildTaskDatas.equals(data.mChildTaskDatas))
                return false;

            if (TextUtils.isEmpty(mNote) != TextUtils.isEmpty(data.mNote))
                return false;

            if (!TextUtils.isEmpty(mNote) && !mNote.equals(data.mNote))
                return false;

            return true;
        }
    }

    public static class ChildTaskData {
        @NonNull
        public final String Name;

        @Nullable
        public final String ScheduleText;

        @NonNull
        public final List<ChildTaskData> Children;

        @Nullable
        public final String mNote;

        @NonNull
        public final ExactTimeStamp mStartExactTimeStamp;

        @NonNull
        public final TaskKey mTaskKey;

        public ChildTaskData(@NonNull String name, @Nullable String scheduleText, @NonNull List<ChildTaskData> children, @Nullable String note, @NonNull ExactTimeStamp startExactTimeStamp, @NonNull TaskKey taskKey) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            Name = name;
            ScheduleText = scheduleText;
            Children = children;
            mNote = note;
            mStartExactTimeStamp = startExactTimeStamp;
            mTaskKey = taskKey;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode += Name.hashCode();
            if (!TextUtils.isEmpty(ScheduleText))
                hashCode += ScheduleText.hashCode();
            hashCode += Children.hashCode();
            if (!TextUtils.isEmpty(mNote))
                hashCode += mNote.hashCode();
            hashCode += mStartExactTimeStamp.hashCode();
            hashCode += mTaskKey.hashCode();
            return hashCode;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof ChildTaskData))
                return false;

            ChildTaskData childTaskData = (ChildTaskData) object;

            if (!Name.equals(childTaskData.Name))
                return false;

            if (TextUtils.isEmpty(ScheduleText) != TextUtils.isEmpty(childTaskData.ScheduleText))
                return false;

            if (!TextUtils.isEmpty(ScheduleText) && !ScheduleText.equals(childTaskData.ScheduleText))
                return false;

            if (!Children.equals(childTaskData.Children))
                return false;

            if (TextUtils.isEmpty(mNote) != TextUtils.isEmpty(childTaskData.mNote))
                return false;

            if (!TextUtils.isEmpty(mNote) && !mNote.equals(childTaskData.mNote))
                return false;

            if (!mStartExactTimeStamp.equals(childTaskData.mStartExactTimeStamp))
                return false;

            if (!mTaskKey.equals(childTaskData.mTaskKey))
                return false;

            return true;
        }
    }
}
