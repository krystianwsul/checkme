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

public class SingleScheduleLoader extends AsyncTaskLoader<SingleScheduleLoader.Data> {
    private Data mData;

    private Observer mObserver;

    private final Integer mRootTaskId; // possibly null

    public SingleScheduleLoader(Context context, Integer rootTaskId) {
        super(context);

        mRootTaskId = rootTaskId;
    }

    @Override
    public Data loadInBackground() {
        Data data = DomainFactory.getDomainFactory(getContext()).getSingleScheduleData(mRootTaskId);
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
        public void onDomainChanged(int dataId) {
            if (mData != null && dataId == mData.DataId)
                return;

            onContentChanged();
        }
    }

    public static class Data extends LoaderData {
        public final ScheduleData ScheduleData;
        public final HashMap<Integer, CustomTimeData> CustomTimeDatas;

        public Data(ScheduleData scheduleData, HashMap<Integer, CustomTimeData> customTimeDatas) {
            Assert.assertTrue(customTimeDatas != null);

            ScheduleData = scheduleData;
            CustomTimeDatas = customTimeDatas;
        }
    }

    public static class ScheduleData {
        public final Date Date;
        public final Integer CustomTimeId;
        public final HourMinute HourMinute;

        public ScheduleData(Date date, Integer customTimeId, HourMinute hourMinute) {
            Assert.assertTrue(date != null);
            Assert.assertTrue((customTimeId == null) != (hourMinute == null));

            Date = date;
            CustomTimeId = customTimeId;
            HourMinute = hourMinute;
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
