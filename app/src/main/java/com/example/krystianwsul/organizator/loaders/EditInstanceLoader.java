package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.HourMinute;

import junit.framework.Assert;

import java.util.HashMap;

public class EditInstanceLoader extends AsyncTaskLoader<EditInstanceLoader.Data> {
    private Data mData;

    private Observer mObserver;

    private final int mTaskId;
    private final Date mDate;
    private final Integer mCustomTimeId;
    private final HourMinute mHourMinute;

    public EditInstanceLoader(Context context, int taskId, Date date, int customTimeId) {
        super(context);

        Assert.assertTrue(date != null);

        mTaskId = taskId;
        mDate = date;
        mCustomTimeId = customTimeId;
        mHourMinute = null;
    }

    public EditInstanceLoader(Context context, int taskId, Date date, HourMinute hourMinute) {
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
        DomainFactory domainFactory = DomainFactory.getDomainFactory(getContext());
        Assert.assertTrue(domainFactory != null);

        if (mCustomTimeId != null) {
            Assert.assertTrue(mHourMinute == null);

            Data data = domainFactory.getEditInstanceData(mTaskId, mDate, mCustomTimeId);
            Assert.assertTrue(data != null);

            return data;
        } else  {
            Assert.assertTrue(mHourMinute != null);

            Data data = domainFactory.getEditInstanceData(mTaskId, mDate, mHourMinute);
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
        public void onDomainChanged(DomainFactory domainFactory, int dataId) {
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
        public final Date InstanceDate;
        public final Integer InstanceCustomTimeId;
        public final HourMinute InstanceHourMinute;
        public final String Name;
        public final HashMap<Integer, CustomTimeData> CustomTimeDatas;

        public Data(int taskId, Date scheduleDate, Integer scheduleCustomTimeId, HourMinute scheduleHourMinute, Date instanceDate, Integer instanceCustomTimeId, HourMinute instanceHourMinute, String name, HashMap<Integer, CustomTimeData> customTimeDatas) {
            Assert.assertTrue(scheduleDate != null);
            Assert.assertTrue((scheduleCustomTimeId == null) != (scheduleHourMinute == null));
            Assert.assertTrue(instanceDate != null);
            Assert.assertTrue((instanceCustomTimeId == null) != (instanceHourMinute == null));
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(customTimeDatas != null);

            TaskId = taskId;
            ScheduleDate = scheduleDate;
            ScheduleCustomTimeId = scheduleCustomTimeId;
            ScheduleHourMinute = scheduleHourMinute;
            InstanceDate = instanceDate;
            InstanceCustomTimeId = instanceCustomTimeId;
            InstanceHourMinute = instanceHourMinute;
            Name = name;
            CustomTimeDatas = customTimeDatas;
        }
    }

    public static class CustomTimeData {
        public final int Id;
        public final String Name;
        public final HashMap<DayOfWeek, HourMinute> HourMinutes;

        public CustomTimeData(int id, String name, HashMap<DayOfWeek, HourMinute> hourMinutes) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(hourMinutes != null);
            Assert.assertTrue(hourMinutes.size() == 7);

            Id = id;
            Name = name;
            HourMinutes = hourMinutes;
        }
    }
}
