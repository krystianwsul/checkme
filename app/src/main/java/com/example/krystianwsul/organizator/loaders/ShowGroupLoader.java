package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowGroupLoader extends DomainLoader<ShowGroupLoader.Data, ShowGroupLoader.Observer> {
    private Observer mObserver;

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

    @Override
    protected ShowGroupLoader.Observer newObserver() {
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
        public final String DisplayText;
        public final ArrayList<InstanceData> InstanceDatas;

        public Data(String displayText, ArrayList<InstanceData> instanceDatas) {
            Assert.assertTrue(displayText != null);
            Assert.assertTrue(instanceDatas != null);
            Assert.assertTrue(!instanceDatas.isEmpty());

            DisplayText = displayText;
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
