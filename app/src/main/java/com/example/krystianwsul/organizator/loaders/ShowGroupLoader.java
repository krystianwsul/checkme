package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
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
        public final HashMap<InstanceKey, InstanceData> InstanceDatas;

        public Data(String displayText, HashMap<InstanceKey, InstanceData> instanceDatas) {
            Assert.assertTrue(!TextUtils.isEmpty(displayText));
            Assert.assertTrue(instanceDatas != null);
            Assert.assertTrue(!instanceDatas.isEmpty());

            DisplayText = displayText;
            InstanceDatas = instanceDatas;
        }

        @Override
        public int hashCode() {
            return (DisplayText.hashCode() + InstanceDatas.hashCode());
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

            return (DisplayText.equals(data.DisplayText) && InstanceDatas.equals(data.InstanceDatas));
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
