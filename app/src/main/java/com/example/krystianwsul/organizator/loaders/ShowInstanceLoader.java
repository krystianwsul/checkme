package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowInstanceLoader extends AsyncTaskLoader<ShowInstanceLoader.Data> {
    private Data mData;

    private Observer mObserver;

    private final int mTaskId;
    private final Date mDate;
    private final Integer mCustomTimeId;
    private final HourMinute mHourMinute;

    public ShowInstanceLoader(Context context, int taskId, Date date, int customTimeId) {
        super(context);

        Assert.assertTrue(date != null);

        mTaskId = taskId;
        mDate = date;
        mCustomTimeId = customTimeId;
        mHourMinute = null;
    }

    public ShowInstanceLoader(Context context, int taskId, Date date, HourMinute hourMinute) {
        super(context);

        Assert.assertTrue(date != null);
        Assert.assertTrue(hourMinute != null);

        mTaskId = taskId;
        mDate = date;
        mCustomTimeId = null;
        mHourMinute = hourMinute;
    }

    @Override
    public Data loadInBackground() {
        if (mCustomTimeId != null) {
            Assert.assertTrue(mHourMinute == null);

            Data data = DomainFactory.getDomainFactory(getContext()).getShowInstanceData(getContext(), mTaskId, mDate, mCustomTimeId);
            Assert.assertTrue(data != null);

            return data;
        } else  {
            Assert.assertTrue(mHourMinute != null);

            Data data = DomainFactory.getDomainFactory(getContext()).getShowInstanceData(getContext(), mTaskId, mDate, mHourMinute);
            Assert.assertTrue(data != null);

            return data;
        }
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
        public void onDomainChanged(int dataId) {
            if (mData != null && dataId == mData.DataId)
                return;

            onContentChanged();
        }
    }

    public static class Data extends LoaderData {
        public final int TaskId;
        public final Date ScheduleDate;
        public final Integer ScheduleCustomTimeId;
        public final HourMinute ScheduleHourMinute;
        public final String Name;
        public final String DisplayText;
        public boolean Done;
        public final boolean HasChildren;
        public final ArrayList<InstanceData> InstanceDatas;

        public Data(int taskId, Date scheduleDate, Integer scheduleCustomTimeId, HourMinute scheduleHourMinute, String name, String displayText, boolean done, boolean hasChildren, ArrayList<InstanceData> instanceDatas) {
            Assert.assertTrue(scheduleDate != null);
            Assert.assertTrue((scheduleCustomTimeId == null) != (scheduleHourMinute == null));
            Assert.assertTrue(!TextUtils.isEmpty(name));

            TaskId = taskId;
            ScheduleDate = scheduleDate;
            ScheduleCustomTimeId = scheduleCustomTimeId;
            ScheduleHourMinute = scheduleHourMinute;
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
