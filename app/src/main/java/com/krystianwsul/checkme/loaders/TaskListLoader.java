package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.List;

public class TaskListLoader extends DomainLoader<TaskListLoader.Data> {
    private final Integer mTaskId;

    public TaskListLoader(Context context, Integer taskId) {
        super(context);
        mTaskId = taskId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getTaskListData(getContext(), mTaskId);
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
        public final int TaskId;
        public final String Name;
        public final String ScheduleText;
        public final List<ChildTaskData> Children;

        public ChildTaskData(int taskId, String name, String scheduleText, List<ChildTaskData> children) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(children != null);

            TaskId = taskId;
            Name = name;
            ScheduleText = scheduleText;
            Children = children;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode += TaskId;
            hashCode += Name.hashCode();
            if (!TextUtils.isEmpty(ScheduleText))
                hashCode += ScheduleText.hashCode();
            hashCode += Children.hashCode();
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

            if (TaskId != childTaskData.TaskId)
                return false;

            if (!Name.equals(childTaskData.Name))
                return false;

            if (TextUtils.isEmpty(ScheduleText) != TextUtils.isEmpty(childTaskData.ScheduleText))
                return false;

            if (!TextUtils.isEmpty(ScheduleText) && !ScheduleText.equals(childTaskData.ScheduleText))
                return false;

            if (!Children.equals(childTaskData.Children))
                return false;

            return true;
        }
    }
}
