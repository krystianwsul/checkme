package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowNotificationGroupLoader extends DomainLoader<ShowNotificationGroupLoader.Data, ShowNotificationGroupLoader.Observer> {
    private final ArrayList<InstanceKey> mInstanceKeys;

    public ShowNotificationGroupLoader(Context context, ArrayList<InstanceKey> instanceKeys) {
        super(context);

        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(!instanceKeys.isEmpty());

        mInstanceKeys = instanceKeys;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getShowNotificationGroupData(getContext(), mInstanceKeys);
    }

    @Override
    protected ShowNotificationGroupLoader.Observer newObserver() {
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
        public final ArrayList<InstanceData> InstanceDatas;

        public Data(ArrayList<InstanceData> instanceDatas) {
            Assert.assertTrue(instanceDatas != null);
            Assert.assertTrue(!instanceDatas.isEmpty());

            InstanceDatas = instanceDatas;
        }

        @Override
        public int hashCode() {
            return InstanceDatas.hashCode();
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

            return InstanceDatas.equals(data.InstanceDatas);
        }
    }

    public static class InstanceData {
        public final TimeStamp Done;
        public final String Name;
        public final boolean HasChildren;
        public final InstanceKey InstanceKey;
        public final String DisplayText;

        public InstanceData(TimeStamp done, String name, boolean hasChildren, InstanceKey instanceKey, String displayText) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(instanceKey != null);
            Assert.assertTrue(!TextUtils.isEmpty(displayText));

            Done = done;
            Name = name;
            HasChildren = hasChildren;
            InstanceKey = instanceKey;
            DisplayText = displayText;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            if (Done != null)
                hashCode += Done.hashCode();
            hashCode += Name.hashCode();
            hashCode += (HasChildren ? 1 : 0);
            hashCode += InstanceKey.hashCode();
            hashCode += DisplayText.hashCode();
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

            return (((Done == null) && (instanceData.Done == null)) || (((Done != null) && (instanceData.Done != null)) && Done.equals(instanceData.Done)) && Name.equals(instanceData.Name) && (HasChildren == instanceData.HasChildren) && InstanceKey.equals(instanceData.InstanceKey) && DisplayText.equals(instanceData.DisplayText));
        }
    }
}
