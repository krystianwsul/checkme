package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.gui.instances.InstanceListFragment;
import com.example.krystianwsul.organizator.utils.InstanceKey;

import junit.framework.Assert;

import java.util.HashMap;

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
        public final boolean HasChildren;
        public final HashMap<InstanceKey, InstanceListFragment.InstanceAdapter.Data> InstanceAdapterDatas;

        public Data(InstanceKey instanceKey, String name, String displayText, boolean done, boolean hasChildren, HashMap<InstanceKey, InstanceListFragment.InstanceAdapter.Data> instanceAdapterDatas) {
            Assert.assertTrue(instanceKey != null);
            Assert.assertTrue(!TextUtils.isEmpty(name));

            InstanceKey = instanceKey;
            Name = name;
            DisplayText = displayText;
            Done = done;
            HasChildren = hasChildren;
            InstanceAdapterDatas = instanceAdapterDatas;
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
            hashCode += InstanceAdapterDatas.hashCode();
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

            return (InstanceKey.equals(data.InstanceKey) && Name.equals(data.Name) && ((TextUtils.isEmpty(DisplayText) && TextUtils.isEmpty(data.DisplayText)) || ((!TextUtils.isEmpty(DisplayText) && !TextUtils.isEmpty(data.DisplayText)) && DisplayText.equals(data.DisplayText))) && (Done == data.Done) && (HasChildren == data.HasChildren) && InstanceAdapterDatas.equals(data.InstanceAdapterDatas));
        }
    }
}
