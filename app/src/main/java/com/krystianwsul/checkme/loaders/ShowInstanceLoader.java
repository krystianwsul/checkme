package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.InstanceKey;

import junit.framework.Assert;

public class ShowInstanceLoader extends DomainLoader<ShowInstanceLoader.Data> {
    @NonNull
    private final InstanceKey mInstanceKey;

    public ShowInstanceLoader(@NonNull Context context, @NonNull InstanceKey instanceKey) {
        super(context);

        mInstanceKey = instanceKey;
    }

    @Override
    String getName() {
        return "ShowInstanceLoader, instanceKey: " + mInstanceKey;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getShowInstanceData(getContext(), mInstanceKey);
    }

    public static class Data extends DomainLoader.Data {
        @Nullable
        public final InstanceData mInstanceData;

        public Data(@Nullable InstanceData instanceData) {
            mInstanceData = instanceData;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            if (mInstanceData != null)
                hashCode += mInstanceData.hashCode();
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

            if ((mInstanceData == null) != (data.mInstanceData == null))
                return false;

            if ((mInstanceData != null) && !mInstanceData.equals(data.mInstanceData))
                return false;

            return true;
        }
    }

    public static class InstanceData {
        @NonNull
        public final InstanceKey InstanceKey;

        @NonNull
        public final String Name;

        @Nullable
        public final String DisplayText;

        public boolean Done;
        public final boolean TaskCurrent;
        public final boolean IsRootInstance;
        public final boolean mExists;

        public InstanceData(@NonNull InstanceKey instanceKey, @NonNull String name, @Nullable String displayText, boolean done, boolean taskCurrent, boolean isRootInstance, boolean exists) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            InstanceKey = instanceKey;
            Name = name;
            DisplayText = displayText;
            Done = done;
            TaskCurrent = taskCurrent;
            IsRootInstance = isRootInstance;
            mExists = exists;
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
            hashCode += (mExists ? 1 : 0);
            return hashCode;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof InstanceData))
                return false;

            InstanceData instanceData = (InstanceData) object;

            if (!InstanceKey.equals(instanceData.InstanceKey))
                return false;

            if (!Name.equals(instanceData.Name))
                return false;

            if (TextUtils.isEmpty(DisplayText) != TextUtils.isEmpty(instanceData.DisplayText))
                return false;

            if (!TextUtils.isEmpty(DisplayText) && !DisplayText.equals(instanceData.DisplayText))
                return false;

            if (Done != instanceData.Done)
                return false;

            if (TaskCurrent != instanceData.TaskCurrent)
                return false;

            if (IsRootInstance != instanceData.IsRootInstance)
                return false;

            if (mExists != instanceData.mExists)
                return false;

            return true;
        }
    }
}
