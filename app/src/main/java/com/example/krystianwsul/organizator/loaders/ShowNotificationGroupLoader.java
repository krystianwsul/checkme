package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowNotificationGroupLoader extends AsyncTaskLoader<ShowNotificationGroupLoader.Data> {
    private Data mData;

    private Observer mObserver;

    private final ArrayList<InstanceKey> mInstanceKeys;

    public ShowNotificationGroupLoader(Context context, ArrayList<InstanceKey> instanceKeys) {
        super(context);

        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(!instanceKeys.isEmpty());

        mInstanceKeys = instanceKeys;
    }

    @Override
    public Data loadInBackground() {
        DomainFactory domainFactory = DomainFactory.getDomainFactory(getContext());
        Assert.assertTrue(domainFactory != null);

        Data data = domainFactory.getShowNotificationGroupData(getContext(), mInstanceKeys);
        Assert.assertTrue(data != null);

        return data;
    }

    @Override
    public void deliverResult(Data data) {
        if (isReset())
            return;

        mData = data;

        if (isStarted())
            super.deliverResult(data);
    }

    @Override
    protected void onStartLoading() {
        if (mData != null)
            deliverResult(mData);

        if (mObserver == null) {
            mObserver = new Observer();
            DomainFactory.addDomainObserver(mObserver);
        }

        if (takeContentChanged() || mData == null)
            forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        onStopLoading();

        if (mData != null)
            mData = null;

        if (mObserver != null) {
            DomainFactory.removeDomainObserver(mObserver);
            mObserver = null;
        }
    }

    private class Observer implements DomainFactory.Observer {
        @Override
        public void onDomainChanged(DomainFactory domainFactory, int dataId) {
            if (mData != null && dataId == mData.DataId)
                return;

            onContentChanged();
        }
    }

    public static class Data extends LoaderData {
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
        public final int TaskId;
        public final Date ScheduleDate;
        public final Integer ScheduleCustomTimeId;
        public final HourMinute ScheduleHourMinute;
        public final String DisplayText;

        public InstanceData(TimeStamp done, String name, boolean hasChildren, int taskId, Date scheduleDate, Integer scheduleCustomTimeId, HourMinute scheduleHourMinute, String displayText) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(scheduleDate != null);
            Assert.assertTrue((scheduleCustomTimeId == null) != (scheduleHourMinute == null));

            Done = done;
            Name = name;
            HasChildren = hasChildren;
            TaskId = taskId;
            ScheduleDate = scheduleDate;
            ScheduleCustomTimeId = scheduleCustomTimeId;
            ScheduleHourMinute = scheduleHourMinute;
            DisplayText = displayText;
        }
    }
}
