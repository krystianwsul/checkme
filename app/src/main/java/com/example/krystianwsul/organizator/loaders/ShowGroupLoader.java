package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.gui.instances.InstanceListFragment;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.HashMap;

public class ShowGroupLoader extends DomainLoader<ShowGroupLoader.Data> {
    private final TimeStamp mTimeStamp;

    public ShowGroupLoader(Context context, TimeStamp timeStamp) {
        super(context);

        Assert.assertTrue(timeStamp != null);
        mTimeStamp = timeStamp;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getShowGroupData(getContext(), mTimeStamp);
    }

    public static class Data extends DomainLoader.Data {
        public final String DisplayText;
        public final HashMap<InstanceKey, InstanceListFragment.InstanceAdapter.Data> InstanceAdapterDatas;

        public Data(String displayText, HashMap<InstanceKey, InstanceListFragment.InstanceAdapter.Data> instanceAdapterDatas) {
            Assert.assertTrue(!TextUtils.isEmpty(displayText));
            Assert.assertTrue(instanceAdapterDatas != null);
            Assert.assertTrue(!instanceAdapterDatas.isEmpty());

            DisplayText = displayText;
            InstanceAdapterDatas = instanceAdapterDatas;
        }

        @Override
        public int hashCode() {
            return (DisplayText.hashCode() + InstanceAdapterDatas.hashCode());
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

            return (DisplayText.equals(data.DisplayText) && InstanceAdapterDatas.equals(data.InstanceAdapterDatas));
        }
    }
}
