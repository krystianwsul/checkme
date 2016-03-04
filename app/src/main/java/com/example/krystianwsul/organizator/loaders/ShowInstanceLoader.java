package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.HashMap;

public class ShowInstanceLoader extends DomainLoader<ShowInstanceLoader.Data, ShowInstanceLoader.Observer> {
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

    @Override
    protected ShowInstanceLoader.Observer newObserver() {
        return new Observer();
    }

    public class Observer implements DomainFactory.Observer {
        @Override
        public void onDomainChanged(int dataId) {
            if (mData != null && dataId == mData.DataId)
                return;

            Data newData = loadInBackground();
            if (mData.equals(newData))
                return;

            onContentChanged();
        }
    }

    public static class Data extends DomainLoader.Data {
        public final InstanceKey InstanceKey;
        public final String Name;
        public final String DisplayText;
        public boolean Done;
        public final boolean HasChildren;
        public final HashMap<InstanceKey, InstanceData> InstanceDatas;

        public Data(InstanceKey instanceKey, String name, String displayText, boolean done, boolean hasChildren, HashMap<InstanceKey, InstanceData> instanceDatas) {
            Assert.assertTrue(instanceKey != null);
            Assert.assertTrue(!TextUtils.isEmpty(name));

            InstanceKey = instanceKey;
            Name = name;
            DisplayText = displayText;
            Done = done;
            HasChildren = hasChildren;
            InstanceDatas = instanceDatas;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode += InstanceKey.hashCode();
            hashCode += Name.hashCode();
            if (!TextUtils.isEmpty(DisplayText))
                hashCode += DisplayText.hashCode();
            hashCode += (Done ? 1 : 0);
            hashCode += (HasChildren ? 1 : 0);
            hashCode += InstanceDatas.hashCode();
            return hashCode;
        }

        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof Data))
                return false;

            Data data = (Data) object;

            return (InstanceKey.equals(data.InstanceKey) && Name.equals(data.Name) && ((TextUtils.isEmpty(DisplayText) && TextUtils.isEmpty(data.DisplayText)) || ((!TextUtils.isEmpty(DisplayText) && !TextUtils.isEmpty(data.DisplayText)) && DisplayText.equals(data.DisplayText))) && (Done == data.Done) && (HasChildren == data.HasChildren) && InstanceDatas.equals(data.InstanceDatas));
        }
    }

    public static class InstanceData {
        public final TimeStamp Done;
        public final String Name;
        public final boolean HasChildren;
        public final InstanceKey InstanceKey;

        public InstanceData(TimeStamp done, String name, boolean hasChildren, InstanceKey instanceKey) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(instanceKey != null);

            Done = done;
            Name = name;
            HasChildren = hasChildren;
            InstanceKey = instanceKey;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            if (Done != null)
                hashCode += Done.hashCode();
            hashCode += Name.hashCode();
            hashCode += (HasChildren ? 1 : 0);
            hashCode += InstanceKey.hashCode();
            return hashCode;
        }

        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof InstanceData))
                return false;

            InstanceData instanceData = (InstanceData) object;

            return (((Done == null) && (instanceData.Done == null)) || (((Done != null) && (instanceData.Done != null)) && Done.equals(instanceData.Done)) && Name.equals(instanceData.Name) && (HasChildren == instanceData.HasChildren) && InstanceKey.equals(instanceData.InstanceKey));
        }
    }
}
