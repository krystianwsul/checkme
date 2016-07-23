package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

public class CreateTaskLoader extends DomainLoader<CreateTaskLoader.Data> {
    private final Integer mTaskId;

    public CreateTaskLoader(Context context, Integer taskId) {
        super(context);

        mTaskId = taskId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getCreateChildTaskData(mTaskId, getContext());
    }

    public static class Data extends DomainLoader.Data {
        public final TaskData TaskData;

        public Data(TaskData taskData) {
            TaskData = taskData;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            if (TaskData != null)
                hash += TaskData.hashCode();
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

            return true;
        }
    }

    public static class TaskData {
        public final String Name;
        public final Integer ParentTaskId;
        public final com.krystianwsul.checkme.utils.ScheduleType ScheduleType;

        public TaskData(String name, Integer parentTaskId, com.krystianwsul.checkme.utils.ScheduleType scheduleType) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue((parentTaskId == null) || (scheduleType == null));

            Name = name;
            ParentTaskId = parentTaskId;
            ScheduleType = scheduleType;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            hash += Name.hashCode();
            if (ParentTaskId != null) {
                Assert.assertTrue(ScheduleType == null);

                hash += ParentTaskId;
            } else {
                Assert.assertTrue(ScheduleType != null);

                hash += ScheduleType.hashCode();
            }
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

            if ((ScheduleType == null) != (taskData.ScheduleType == null))
                return false;

            if ((ScheduleType != null) && !ScheduleType.equals(taskData.ScheduleType))
                return false;

            return true;
        }
    }
}
