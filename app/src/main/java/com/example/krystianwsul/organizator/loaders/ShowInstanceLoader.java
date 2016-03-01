package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

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

            onContentChanged();
        }
    }

    public static class Data extends DomainLoader.Data {
        public final InstanceKey InstanceKey;
        public final String Name;
        public final String DisplayText;
        public boolean Done;
        public final boolean HasChildren;
        public final ArrayList<InstanceData> InstanceDatas;

        public Data(InstanceKey instanceKey, String name, String displayText, boolean done, boolean hasChildren, ArrayList<InstanceData> instanceDatas) {
            Assert.assertTrue(instanceKey != null);
            Assert.assertTrue(!TextUtils.isEmpty(name));

            InstanceKey = instanceKey;
            Name = name;
            DisplayText = displayText;
            Done = done;
            HasChildren = hasChildren;
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
