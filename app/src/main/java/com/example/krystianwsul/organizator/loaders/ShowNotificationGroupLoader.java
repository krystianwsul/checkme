package com.example.krystianwsul.organizator.loaders;

import android.content.Context;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.gui.instances.InstanceListFragment;
import com.example.krystianwsul.organizator.utils.InstanceKey;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowNotificationGroupLoader extends DomainLoader<ShowNotificationGroupLoader.Data> {
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

    public static class Data extends DomainLoader.Data {
        public final ArrayList<InstanceListFragment.InstanceAdapter.Data> InstanceAdapterDatas;

        public Data(ArrayList<InstanceListFragment.InstanceAdapter.Data> instanceAdapterDatas) {
            Assert.assertTrue(instanceAdapterDatas != null);
            Assert.assertTrue(!instanceAdapterDatas.isEmpty());

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
