package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

public class GroupListLoader extends DomainLoader<GroupListLoader.Data, GroupListLoader.Observer> {
    public GroupListLoader(Context context) {
        super(context);
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getGroupListData(getContext());
    }

    @Override
    protected GroupListLoader.Observer newObserver() {
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
        public final ArrayList<CustomTimeData> CustomTimeDatas;

        public Data(ArrayList<InstanceData> instanceDatas, ArrayList<CustomTimeData> customTimeDatas) {
            Assert.assertTrue(instanceDatas != null);

            InstanceDatas = instanceDatas;
            CustomTimeDatas = customTimeDatas;
        }
    }

    public static class InstanceData {
        public TimeStamp Done;
        public final InstanceKey InstanceKey;
        public final boolean HasChildren;
        public final String DisplayText;
        public final String Name;
        public final TimeStamp InstanceTimeStamp;

        public InstanceData(TimeStamp done, boolean hasChildren, InstanceKey instanceKey, String displayText, String name, TimeStamp instanceTimeStamp) {
            Assert.assertTrue(instanceKey != null);
            Assert.assertTrue(!TextUtils.isEmpty(displayText));
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(instanceTimeStamp != null);

            Done = done;
            HasChildren = hasChildren;
            InstanceKey = instanceKey;
            DisplayText = displayText;
            Name = name;
            InstanceTimeStamp = instanceTimeStamp;
        }
    }

    public static class CustomTimeData {
        public final String Name;
        public final HashMap<DayOfWeek, HourMinute> HourMinutes;

        public CustomTimeData(String name, HashMap<DayOfWeek, HourMinute> hourMinutes) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(hourMinutes != null);
            Assert.assertTrue(hourMinutes.size() == 7);

            Name = name;
            HourMinutes = hourMinutes;
        }
    }
}
