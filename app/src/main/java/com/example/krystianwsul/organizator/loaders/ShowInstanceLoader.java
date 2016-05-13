package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.InstanceKey;

import junit.framework.Assert;

public class ShowInstanceLoader extends DomainLoader<ShowInstanceLoader.Data> {
    private final InstanceKey mInstanceKey;

    public ShowInstanceLoader(Context context, InstanceKey instanceKey) {
        super(context);

        Assert.assertTrue(instanceKey != null);

        mInstanceKey = instanceKey;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getShowInstanceData(getContext(), mInstanceKey);
    }

    public static class Data extends DomainLoader.Data {
        public final InstanceKey InstanceKey;
        public final String Name;
        public final String DisplayText;
        public boolean Done;
        public final boolean TaskCurrent;
        public final boolean IsRootInstance;
        public final Boolean IsRootTask;

        public Data(InstanceKey instanceKey, String name, String displayText, boolean done, boolean taskCurrent, boolean isRootInstance, Boolean isRootTask) {
            Assert.assertTrue(instanceKey != null);
            Assert.assertTrue(!TextUtils.isEmpty(name));

            InstanceKey = instanceKey;
            Name = name;
            DisplayText = displayText;
            Done = done;
            TaskCurrent = taskCurrent;
            IsRootInstance = isRootInstance;
            IsRootTask = isRootTask;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode += InstanceKey.hashCode();
            hashCode += Name.hashCode();
            if (!TextUtils.isEmpty(DisplayText))
                hashCode += DisplayText.hashCode();
            hashCode += (Done ? 1 : 0);
            hashCode += (TaskCurrent ? 1 : 0);
            hashCode += (IsRootInstance ? 1 : 0);
            if (IsRootTask != null)
            hashCode += (IsRootTask ? 2 : 1);
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

            if (!InstanceKey.equals(data.InstanceKey))
                return false;

            if (!Name.equals(data.Name))
                return false;

            if (TextUtils.isEmpty(DisplayText) != TextUtils.isEmpty(data.DisplayText))
                return false;

            if (!TextUtils.isEmpty(data.DisplayText) && !DisplayText.equals(data.DisplayText))
                return false;

            if (Done != data.Done)
                return false;

            if (TaskCurrent != data.TaskCurrent)
                return false;

            if (IsRootInstance != data.IsRootInstance)
                return false;

            if ((IsRootTask == null) != (data.IsRootTask == null))
                return false;

            if (IsRootTask != null && !IsRootTask.equals(data.IsRootTask))
                return false;

            return true;
        }
    }
}
