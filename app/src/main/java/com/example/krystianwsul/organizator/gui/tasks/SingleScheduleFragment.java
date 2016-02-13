package com.example.krystianwsul.organizator.gui.tasks;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.CustomTime;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.domainmodel.SingleSchedule;
import com.example.krystianwsul.organizator.domainmodel.Task;
import com.example.krystianwsul.organizator.loaders.DomainLoader;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DateTime;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.NormalTime;
import com.example.krystianwsul.organizator.utils.time.Time;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

public class SingleScheduleFragment extends Fragment implements DatePickerFragment.DatePickerFragmentListener, HourMinutePickerFragment.HourMinutePickerFragmentListener, ScheduleFragment, LoaderManager.LoaderCallbacks<DomainFactory> {
    private static final String YEAR_KEY = "year";
    private static final String MONTH_KEY = "month";
    private static final String DAY_KEY = "day";

    private static final String ROOT_TASK_ID_KEY = "rootTaskId";

    private DomainFactory mDomainFactory;
    private Bundle mSavedInstanceState;

    private TextView mDateView;
    private TimePickerView mTimePickerView;

    private Date mDate;

    public static SingleScheduleFragment newInstance() {
        return new SingleScheduleFragment();
    }

    public static SingleScheduleFragment newInstance(Task rootTask) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(rootTask.getCurrentSchedule(TimeStamp.getNow()) != null);
        Assert.assertTrue(rootTask.getCurrentSchedule(TimeStamp.getNow()) instanceof SingleSchedule);

        SingleScheduleFragment singleScheduleFragment = new SingleScheduleFragment();

        Bundle args = new Bundle();
        args.putInt(ROOT_TASK_ID_KEY, rootTask.getId());

        singleScheduleFragment.setArguments(args);
        return singleScheduleFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Assert.assertTrue(context instanceof DatePickerFragment.DatePickerFragmentListener);
        Assert.assertTrue(context instanceof HourMinutePickerFragment.HourMinutePickerFragmentListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_single_schedule, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSavedInstanceState = savedInstanceState;

        View view = getView();
        Assert.assertTrue(view != null);

        mTimePickerView = (TimePickerView) view.findViewById(R.id.single_schedule_timepickerview);
        mTimePickerView.setOnTimeSelectedListener(new TimePickerView.OnTimeSelectedListener() {
            @Override
            public void onCustomTimeSelected(int customTimeId) {
            }

            @Override
            public void onHourMinuteSelected(HourMinute hourMinute) {
            }

            @Override
            public void onHourMinuteClick() {
                FragmentManager fragmentManager = getChildFragmentManager();
                HourMinutePickerFragment hourMinutePickerFragment = HourMinutePickerFragment.newInstance(getActivity(), mTimePickerView.getHourMinute());
                hourMinutePickerFragment.show(fragmentManager, "time");
            }
        });

        mDateView = (TextView) view.findViewById(R.id.single_schedule_date);
        mDateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getChildFragmentManager();
                DatePickerFragment datePickerFragment = DatePickerFragment.newInstance(getActivity(), mDate);
                datePickerFragment.show(fragmentManager, "date");
            }
        });

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Assert.assertTrue(mDate != null);

        outState.putInt(YEAR_KEY, mDate.getYear());
        outState.putInt(MONTH_KEY, mDate.getMonth());
        outState.putInt(DAY_KEY, mDate.getDay());
    }

    private void updateDateText() {
        Assert.assertTrue(mDate != null);
        Assert.assertTrue(mDateView != null);

        mDateView.setText(mDate.getDisplayText(getContext()));
    }

    @Override
    public void onDatePickerFragmentResult(Date date) {
        Assert.assertTrue(date != null);

        mDate = date;
        updateDateText();
    }

    @Override
    public void onHourMinutePickerFragmentResult(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);
        mTimePickerView.setHourMinute(hourMinute);
    }

    @Override
    public boolean isValidTime() {
        HourMinute hourMinute = mTimePickerView.getHourMinute();
        Integer customTimeId = mTimePickerView.getCustomTimeId();
        Assert.assertTrue((hourMinute == null) != (customTimeId == null));

        if (hourMinute == null)
            hourMinute = mDomainFactory.getCustomTime(customTimeId).getHourMinute(mDate.getDayOfWeek());

        return (new TimeStamp(mDate, hourMinute).compareTo(TimeStamp.getNow()) > 0);
    }

    @Override
    public void createRootTask(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        HourMinute hourMinute = mTimePickerView.getHourMinute();
        Integer customTimeId = mTimePickerView.getCustomTimeId();

        Time time;
        if (hourMinute != null) {
            Assert.assertTrue(customTimeId == null);
            time = new NormalTime(hourMinute);
        } else {
            Assert.assertTrue(customTimeId != null);
            time = mDomainFactory.getCustomTime(customTimeId);
            Assert.assertTrue(time != null);
        }

        mDomainFactory.createSingleScheduleRootTask(name, new DateTime(mDate, time));

        mDomainFactory.save();

        TickService.startService(getActivity());
    }

    @Override
    public void updateRootTask(int rootTaskId, String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Task rootTask = mDomainFactory.getTask(rootTaskId);
        Assert.assertTrue(rootTask != null);

        HourMinute hourMinute = mTimePickerView.getHourMinute();
        Integer customTimeId = mTimePickerView.getCustomTimeId();

        Time time;
        if (hourMinute != null) {
            Assert.assertTrue(customTimeId == null);
            time = new NormalTime(hourMinute);
        } else {
            Assert.assertTrue(customTimeId != null);
            time = mDomainFactory.getCustomTime(customTimeId);
            Assert.assertTrue(time != null);
        }

        mDomainFactory.updateSingleScheduleRootTask(rootTask, name, new DateTime(mDate, time));

        mDomainFactory.save();

        TickService.startService(getActivity());
    }

    @Override
    public void createRootJoinTask(String name, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        ArrayList<Task> joinTasks = new ArrayList<>();
        for (Integer joinTaskId : joinTaskIds) {
            Task joinTask = mDomainFactory.getTask(joinTaskId);
            Assert.assertTrue(joinTask != null);

            joinTasks.add(joinTask);
        }

        HourMinute hourMinute = mTimePickerView.getHourMinute();
        Integer customTimeId = mTimePickerView.getCustomTimeId();

        Time time;
        if (hourMinute != null) {
            Assert.assertTrue(customTimeId == null);
            time = new NormalTime(hourMinute);
        } else {
            Assert.assertTrue(customTimeId != null);
            time = mDomainFactory.getCustomTime(customTimeId);
            Assert.assertTrue(time != null);
        }

        mDomainFactory.createSingleScheduleJoinRootTask(name, new DateTime(mDate, time), joinTasks);

        mDomainFactory.save();

        TickService.startService(getActivity());
    }

    @Override
    public Loader<DomainFactory> onCreateLoader(int id, Bundle args) {
        return new DomainLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<DomainFactory> loader, DomainFactory domainFactory) {
        mDomainFactory = domainFactory;

        Bundle args = getArguments();

        Integer customTimeId = null;
        HourMinute hourMinute = null;
        if (mSavedInstanceState != null) {
            int year = mSavedInstanceState.getInt(YEAR_KEY, -1);
            int month = mSavedInstanceState.getInt(MONTH_KEY, -1);
            int day = mSavedInstanceState.getInt(DAY_KEY, -1);

            Assert.assertTrue(year != -1);
            Assert.assertTrue(month != -1);
            Assert.assertTrue(day != -1);

            mDate = new Date(year, month, day);
        } else if (args != null) {
            Assert.assertTrue(args.containsKey(ROOT_TASK_ID_KEY));
            int rootTaskId = args.getInt(ROOT_TASK_ID_KEY, -1);
            Assert.assertTrue(rootTaskId != -1);

            Task rootTask = domainFactory.getTask(rootTaskId);
            Assert.assertTrue(rootTask != null);

            SingleSchedule singleSchedule = (SingleSchedule) rootTask.getCurrentSchedule(TimeStamp.getNow());
            Assert.assertTrue(singleSchedule != null);
            Assert.assertTrue(singleSchedule.current(TimeStamp.getNow()));

            DateTime dateTime = singleSchedule.getDateTime();
            mDate = dateTime.getDate();
            if (dateTime.getTime() instanceof CustomTime)
                customTimeId = ((CustomTime) dateTime.getTime()).getId();
            else
                hourMinute = ((NormalTime) dateTime.getTime()).getHourMinute();
        } else {
            mDate = Date.today();
        }

        updateDateText();

        HashMap<Integer, TimePickerView.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : mDomainFactory.getCurrentCustomTimes())
            customTimeDatas.put(customTime.getId(), new TimePickerView.CustomTimeData(customTime.getId(), customTime.getName(), customTime.getHourMinutes()));
        mTimePickerView.setCustomTimeDatas(customTimeDatas);

        if (customTimeId != null)
            mTimePickerView.setCustomTimeId(customTimeId);
        else if (hourMinute != null)
            mTimePickerView.setHourMinute(hourMinute);
    }

    @Override
    public void onLoaderReset(Loader<DomainFactory> loader) {
        mDomainFactory = null;
    }
}