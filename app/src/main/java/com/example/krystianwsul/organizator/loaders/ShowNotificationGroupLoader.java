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

            Done = done;
            Name = name;
            HasChildren = hasChildren;
            InstanceKey = instanceKey;
            DisplayText = displayText;
        }
    }
}
