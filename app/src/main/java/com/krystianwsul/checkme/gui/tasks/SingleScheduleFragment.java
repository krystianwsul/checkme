package com.krystianwsul.checkme.gui.tasks;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity;
import com.krystianwsul.checkme.loaders.SingleScheduleLoader;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePairPersist;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.List;

public class SingleScheduleFragment extends Fragment implements ScheduleFragment, LoaderManager.LoaderCallbacks<SingleScheduleLoader.Data> {
    private static final String SCHEDULE_HINT_KEY = "scheduleHint";

    private static final String INITIAL_HOUR_MINUTE_KEY = "initialHourMinute";

    private static final String PARCEL_DATE_KEY = "date";
    private static final String TIME_PAIR_PERSIST_KEY = "timePairPersist";

    private static final String ROOT_TASK_ID_KEY = "rootTaskId";

    private static final String SINGLE_SCHEDULE_DIALOG_TAG = "singleScheduleDialog";

    private Integer mRootTaskId;
    private SingleScheduleLoader.Data mData;

    private TextInputLayout mSingleScheduleLayout;
    private TextView mSingleScheduleText;

    private Date mDate;
    private TimePairPersist mTimePairPersist;

    private BroadcastReceiver mBroadcastReceiver;

    private Bundle mSavedInstanceState;

    private boolean mFirst = true;

    private final SingleScheduleDialogFragment.SingleScheduleDialogListener mSingleScheduleDialogListener = new SingleScheduleDialogFragment.SingleScheduleDialogListener() {
        @Override
        public void onSingleScheduleDialogResult(Date date, TimePairPersist timePairPersist) {
            Assert.assertTrue(date != null);
            Assert.assertTrue(timePairPersist != null);

            mDate = date;
            mTimePairPersist = timePairPersist;

            updateText();
        }
    };

    private HourMinute mInitialHourMinute;

    public static SingleScheduleFragment newInstance() {
        return new SingleScheduleFragment();
    }

    public static SingleScheduleFragment newInstance(CreateTaskActivity.ScheduleHint scheduleHint) {
        Assert.assertTrue(scheduleHint != null);

        SingleScheduleFragment singleScheduleFragment = new SingleScheduleFragment();

        Bundle args = new Bundle();
        args.putParcelable(SCHEDULE_HINT_KEY, scheduleHint);
        singleScheduleFragment.setArguments(args);

        return singleScheduleFragment;
    }

    public static SingleScheduleFragment newInstance(int rootTaskId) {
        SingleScheduleFragment singleScheduleFragment = new SingleScheduleFragment();

        Bundle args = new Bundle();
        args.putInt(ROOT_TASK_ID_KEY, rootTaskId);
        singleScheduleFragment.setArguments(args);

        return singleScheduleFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_single_schedule, container, false);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSavedInstanceState = savedInstanceState;

        View view = getView();
        Assert.assertTrue(view != null);

        mSingleScheduleLayout = (TextInputLayout) view.findViewById(R.id.single_schedule_layout);
        Assert.assertTrue(mSingleScheduleLayout != null);

        mSingleScheduleText = (TextView) view.findViewById(R.id.single_schedule_text);
        Assert.assertTrue(mSingleScheduleText != null);

        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(ROOT_TASK_ID_KEY)) {
                Assert.assertTrue(!args.containsKey(SCHEDULE_HINT_KEY));

                mRootTaskId = args.getInt(ROOT_TASK_ID_KEY, -1);
                Assert.assertTrue(mRootTaskId != -1);
            } else {
                Assert.assertTrue(args.containsKey(SCHEDULE_HINT_KEY));
            }
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(PARCEL_DATE_KEY)) {
            Assert.assertTrue(savedInstanceState.containsKey(TIME_PAIR_PERSIST_KEY));
            Assert.assertTrue(savedInstanceState.containsKey(INITIAL_HOUR_MINUTE_KEY));

            mDate = savedInstanceState.getParcelable(PARCEL_DATE_KEY);
            Assert.assertTrue(mDate != null);

            mTimePairPersist = savedInstanceState.getParcelable(TIME_PAIR_PERSIST_KEY);
            Assert.assertTrue(mTimePairPersist != null);

            mInitialHourMinute = savedInstanceState.getParcelable(INITIAL_HOUR_MINUTE_KEY);
        } else if (args != null) {
            if (!args.containsKey(ROOT_TASK_ID_KEY)) {
                Assert.assertTrue(args.containsKey(SCHEDULE_HINT_KEY));

                CreateTaskActivity.ScheduleHint scheduleHint = args.getParcelable(SCHEDULE_HINT_KEY);
                Assert.assertTrue(scheduleHint != null);

                mDate = scheduleHint.mDate;

                if (scheduleHint.mTimePair != null) {
                    mTimePairPersist = new TimePairPersist(scheduleHint.mTimePair);
                } else {
                    mTimePairPersist = new TimePairPersist();
                }
                mInitialHourMinute = mTimePairPersist.getHourMinute();
            }
        } else {
            mDate = Date.today();
            mTimePairPersist = new TimePairPersist();
            mInitialHourMinute = mTimePairPersist.getHourMinute();
        }

        getLoaderManager().initLoader(0, null, this);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mData != null)
                    setValidTime();
            }
        };
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("SingleScheduleFragment.onResume");

        super.onResume();

        getActivity().registerReceiver(mBroadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));

        if (mData != null)
            setValidTime();
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mData != null) {
            Assert.assertTrue(mDate != null);
            Assert.assertTrue(mTimePairPersist != null);

            outState.putParcelable(PARCEL_DATE_KEY, mDate);
            outState.putParcelable(TIME_PAIR_PERSIST_KEY, mTimePairPersist);
            outState.putParcelable(INITIAL_HOUR_MINUTE_KEY, mInitialHourMinute);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateText() {
        Assert.assertTrue(mData != null);
        Assert.assertTrue(mDate != null);
        Assert.assertTrue(mTimePairPersist != null);
        Assert.assertTrue(mSingleScheduleText != null);

        String dateText = mDate.getDisplayText(getContext());
        String timeText;

        if (mTimePairPersist.getCustomTimeId() != null) {
            SingleScheduleLoader.CustomTimeData customTimeData = mData.CustomTimeDatas.get(mTimePairPersist.getCustomTimeId());
            Assert.assertTrue(customTimeData != null);

            timeText = customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDate.getDayOfWeek()) + ")";
        } else {
            timeText = mTimePairPersist.getHourMinute().toString();
        }

        mSingleScheduleText.setText(dateText + ", " + timeText);

        setValidTime();
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean isValidDate() {
        if (mData != null) {
            return (mDate.compareTo(Date.today()) >= 0);
        } else {
            return false;
        }
    }

    private boolean isValidDateTime() {
        if (mData != null) {
            if (mData.ScheduleDatas != null && mData.ScheduleDatas.get(0).TimePair.equals(mTimePairPersist.getTimePair())) {
                return true;
            } else {
                HourMinute hourMinute;
                if (mTimePairPersist.getCustomTimeId() != null) {
                    if (!mData.CustomTimeDatas.containsKey(mTimePairPersist.getCustomTimeId()))
                        return false; //cached data doesn't contain new custom time

                    hourMinute = mData.CustomTimeDatas.get(mTimePairPersist.getCustomTimeId()).HourMinutes.get(mDate.getDayOfWeek());
                } else {
                    hourMinute = mTimePairPersist.getHourMinute();
                }

                return (new TimeStamp(mDate, hourMinute).compareTo(TimeStamp.getNow()) > 0);
            }
        } else {
            return false;
        }
    }

    private void setValidTime() {
        if (isValidDateTime()) {
            mSingleScheduleLayout.setError(null);
        } else {
            if (isValidDate()) {
                mSingleScheduleLayout.setError(getString(R.string.error_time));
            } else {
                mSingleScheduleLayout.setError(getString(R.string.error_date));
            }
        }
    }

    @Override
    public boolean createRootTask(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Assert.assertTrue(mRootTaskId == null);

        if (!isValidDateTime())
            return false;

        DomainFactory.getDomainFactory(getActivity()).createSingleScheduleRootTask(mData.DataId, name, mDate, mTimePairPersist.getTimePair());

        TickService.startService(getActivity());

        return true;
    }

    @Override
    public boolean updateRootTask(int rootTaskId, String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        if (!isValidDateTime())
            return false;

        DomainFactory.getDomainFactory(getActivity()).updateSingleScheduleTask(mData.DataId, rootTaskId, name, mDate, mTimePairPersist.getTimePair());

        TickService.startService(getActivity());

        return true;
    }

    @Override
    public boolean createRootJoinTask(String name, List<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        Assert.assertTrue(mRootTaskId == null);

        if (!isValidDateTime())
            return false;

        DomainFactory.getDomainFactory(getActivity()).createSingleScheduleJoinRootTask(mData.DataId, name, mDate, mTimePairPersist.getTimePair(), joinTaskIds);

        TickService.startService(getActivity());

        return true;
    }

    @Override
    public Loader<SingleScheduleLoader.Data> onCreateLoader(int id, Bundle args) {
        return new SingleScheduleLoader(getActivity(), mRootTaskId);
    }

    @Override
    public void onLoadFinished(Loader<SingleScheduleLoader.Data> loader, SingleScheduleLoader.Data data) {
        mData = data;

        if (mFirst && mData.ScheduleDatas != null && (mSavedInstanceState == null || !mSavedInstanceState.containsKey(PARCEL_DATE_KEY))) {
            Assert.assertTrue(mDate == null);
            Assert.assertTrue(mTimePairPersist == null);
            Assert.assertTrue(mData.ScheduleDatas.size() == 1); // todo schedule hack

            mFirst = false;

            mDate = mData.ScheduleDatas.get(0).Date;
            mTimePairPersist = new TimePairPersist(mData.ScheduleDatas.get(0).TimePair);
        }

        mSingleScheduleLayout.setVisibility(View.VISIBLE);

        updateText();

        mSingleScheduleText.setOnClickListener(v -> {
            Assert.assertTrue(mData != null);

            SingleScheduleDialogFragment singleScheduleDialogFragment = SingleScheduleDialogFragment.newInstance(mDate, mTimePairPersist);
            singleScheduleDialogFragment.initialize(mData.CustomTimeDatas, mSingleScheduleDialogListener);

            singleScheduleDialogFragment.show(getChildFragmentManager(), SINGLE_SCHEDULE_DIALOG_TAG);
        });

        SingleScheduleDialogFragment singleScheduleDialogFragment = (SingleScheduleDialogFragment) getChildFragmentManager().findFragmentByTag(SINGLE_SCHEDULE_DIALOG_TAG);
        if (singleScheduleDialogFragment != null)
            singleScheduleDialogFragment.initialize(mData.CustomTimeDatas, mSingleScheduleDialogListener);
    }

    @Override
    public void onLoaderReset(Loader<SingleScheduleLoader.Data> loader) {}

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean dataChanged() {
        if (mRootTaskId == null) {
            if (mData == null)
                return false;

            Assert.assertTrue(mData.ScheduleDatas == null);

            Bundle args = getArguments();

            Date initialDate;
            if (args != null && args.containsKey(SCHEDULE_HINT_KEY)) {
                CreateTaskActivity.ScheduleHint scheduleHint = args.getParcelable(SCHEDULE_HINT_KEY);
                Assert.assertTrue(scheduleHint != null);

                initialDate = scheduleHint.mDate;
            } else {
                initialDate = Date.today();
            }

            if (!mDate.equals(initialDate))
                return true;

            if (mTimePairPersist.getCustomTimeId() != null)
                return true;

            Assert.assertTrue(mInitialHourMinute != null);

            if (!mInitialHourMinute.equals(mTimePairPersist.getHourMinute()))
                return true;

            return false;
        } else {
            if (mData == null)
                return false;

            Assert.assertTrue(mData.ScheduleDatas != null);
            Assert.assertTrue(mData.ScheduleDatas.size() == 1);

            if (!mData.ScheduleDatas.get(0).Date.equals(mDate))
                return true;

            if (!mData.ScheduleDatas.get(0).TimePair.equals(mTimePairPersist.getTimePair()))
                return true;

            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Assert.assertTrue(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE);
        Assert.assertTrue(resultCode >= 0);
        Assert.assertTrue(data == null);

        Assert.assertTrue(mTimePairPersist != null);

        if (resultCode > 1)
            mTimePairPersist.setCustomTimeId(resultCode);
    }
}