package com.example.krystianwsul.organizator.loaders;

import android.content.Context;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.gui.instances.InstanceListFragment;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

public class InstanceListLoader extends DomainLoader<InstanceListLoader.Data> {
    private final TimeStamp mTimeStamp;
    private final InstanceKey mInstanceKey;
    private final ArrayList<InstanceKey> mInstanceKeys;

    public InstanceListLoader(Context context, TimeStamp timeStamp, InstanceKey instanceKey, ArrayList<InstanceKey> instanceKeys) {
        super(context);

        Assert.assertTrue((timeStamp != null ? 1 : 0) + (instanceKey != null ? 1 : 0) + (instanceKeys != null ? 1 : 0) == 1);

        mTimeStamp = timeStamp;
        mInstanceKey = instanceKey;
        mInstanceKeys = instanceKeys;
    }

    @Override
    public Data loadInBackground() {
        if (mTimeStamp != null) {
            Assert.assertTrue(mInstanceKey == null);
            Assert.assertTrue(mInstanceKeys == null);

            return DomainFactory.getDomainFactory(getContext()).getInstanceListData(getContext(), mTimeStamp);
        } else if (mInstanceKey != null) {
            Assert.assertTrue(mInstanceKeys == null);

            return DomainFactory.getDomainFactory(getContext()).getInstanceListData(getContext(), mInstanceKey);
        } else {
            Assert.assertTrue(mInstanceKeys != null);
            Assert.assertTrue(!mInstanceKeys.isEmpty());

            return DomainFactory.getDomainFactory(getContext()).getInstanceListData(getContext(), mInstanceKeys);
        }
    }

    public static class Data extends DomainLoader.Data {
        public final HashMap<InstanceKey, InstanceListFragment.InstanceAdapter.Data> InstanceAdapterDatas;

        public Data(HashMap<InstanceKey, InstanceListFragment.InstanceAdapter.Data> instanceAdapterDatas) {
            Assert.assertTrue(instanceAdapterDatas != null);
            InstanceAdapterDatas = instanceAdapterDatas;
        }

        @Override
        public int hashCode() {
            return InstanceAdapterDatas.hashCode();
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

            return InstanceAdapterDatas.equals(data.InstanceAdapterDatas);
        }
    }
}
