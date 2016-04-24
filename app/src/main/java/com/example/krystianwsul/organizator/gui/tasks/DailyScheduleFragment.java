package com.example.krystianwsul.organizator.gui.tasks;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.loaders.DailyScheduleLoader;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimePair;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DailyScheduleFragment extends Fragment implements ScheduleFragment, LoaderManager.LoaderCallbacks<DailyScheduleLoader.Data> {
    private static final String HOUR_MINUTE_KEY = "hourMinute";
    private static final String ROOT_TASK_ID_KEY = "rootTaskId";

    private static final String TIME_ENTRY_KEY = "timeEntries";
    private static final String HOUR_MINUTE_PICKER_POSITION_KEY = "hourMinutePickerPosition";

    private static final String TIME_PICKER_TAG = "timePicker";

    private int mHourMinutePickerPosition = -1;

    private TimeEntryAdapter mTimeEntryAdapter;
    private RecyclerView mDailyScheduleTimes;

    private Bundle mSavedInstanceState;

    private HourMinute mHourMinute = HourMinute.getNow();

    private Integer mRootTaskId;
    private DailyScheduleLoader.Data mData;

    private final RadialTimePickerDialogFragment.OnTimeSetListener mOnTimeSetListener = new RadialTimePickerDialogFragment.OnTimeSetListener() {
        @Override
        public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
            Assert.assertTrue(mHourMinutePickerPosition != -1);

            TimeEntry timeEntry = mTimeEntryAdapter.getTimeEntry(mHourMinutePickerPosition);
            Assert.assertTrue(timeEntry != null);

            timeEntry.setTimePair(new TimePair(new HourMinute(hourOfDay, minute)));
            mTimeEntryAdapter.notifyItemChanged(mHourMinutePickerPosition);

            mHourMinutePickerPosition = -1;
        }
    };

    public static DailyScheduleFragment newInstance() {
        return new DailyScheduleFragment();
    }

    public static DailyScheduleFragment newInstance(HourMinute hourMinute) {
        DailyScheduleFragment dailyScheduleFragment = new DailyScheduleFragment();

        Bundle args = new Bundle();
        args.putParcelable(HOUR_MINUTE_KEY, hourMinute);

        dailyScheduleFragment.setArguments(args);
        return dailyScheduleFragment;
    }

    public static DailyScheduleFragment newInstance(int rootTaskId) {
        DailyScheduleFragment dailyScheduleFragment = new DailyScheduleFragment();

        Bundle args = new Bundle();
        args.putInt(ROOT_TASK_ID_KEY, rootTaskId);

        dailyScheduleFragment.setArguments(args);
        return dailyScheduleFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_daily_schedule, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSavedInstanceState = savedInstanceState;

        View view = getView();
        Assert.assertTrue(view != null);

        mDailyScheduleTimes = (RecyclerView) view.findViewById(R.id.daily_schedule_times);
        mDailyScheduleTimes.setLayoutManager(new LinearLayoutManager(getContext()));

        Bundle args = getArguments();
        if (args != null)
        {
            if (args.containsKey(ROOT_TASK_ID_KEY)) {
                Assert.assertTrue(!args.containsKey(HOUR_MINUTE_KEY));

                mRootTaskId = args.getInt(ROOT_TASK_ID_KEY, -1);
                Assert.assertTrue(mRootTaskId != -1);
            } else {
                Assert.assertTrue(args.containsKey(HOUR_MINUTE_KEY));

                mHourMinute = args.getParcelable(HOUR_MINUTE_KEY);
                Assert.assertTrue(mHourMinute != null);
            }
        }

        FloatingActionButton dailyScheduleFab = (FloatingActionButton) view.findViewById(R.id.daily_schedule_fab);
        Assert.assertTrue(dailyScheduleFab != null);

        dailyScheduleFab.setOnClickListener(v -> {
            Assert.assertTrue(mTimeEntryAdapter != null);
            mTimeEntryAdapter.addTimeEntry();
        });

        ((CreateRootTaskActivity) getActivity()).setTimeValid(true);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<DailyScheduleLoader.Data> onCreateLoader(int id, Bundle args) {
        return new DailyScheduleLoader(getActivity(), mRootTaskId);
    }

    @Override
    public void onLoadFinished(Loader<DailyScheduleLoader.Data> loader, DailyScheduleLoader.Data data) {
        mData = data;

        Bundle args = getArguments();
        if (mSavedInstanceState != null) {
            List<TimeEntry> timeEntries = mSavedInstanceState.getParcelableArrayList(TIME_ENTRY_KEY);
            mTimeEntryAdapter = new TimeEntryAdapter(getContext(), timeEntries);

            mHourMinutePickerPosition = mSavedInstanceState.getInt(HOUR_MINUTE_PICKER_POSITION_KEY, -2);
            Assert.assertTrue(mHourMinutePickerPosition != -2);
        } else if (args != null && args.containsKey(ROOT_TASK_ID_KEY)) {
            Assert.assertTrue(mData.ScheduleDatas != null);
            Assert.assertTrue(!mData.ScheduleDatas.isEmpty());

            ArrayList<TimeEntry> timeEntries = new ArrayList<>();
            boolean showDelete = (mData.ScheduleDatas.size() > 1);
            for (DailyScheduleLoader.ScheduleData scheduleData : mData.ScheduleDatas)
                timeEntries.add(new TimeEntry(scheduleData.TimePair, showDelete));

            mTimeEntryAdapter = new TimeEntryAdapter(getContext(), timeEntries);

            mHourMinutePickerPosition = -1;
        } else {
            mTimeEntryAdapter = new TimeEntryAdapter(getContext());
            mHourMinutePickerPosition = -1;
        }
        mDailyScheduleTimes.setAdapter(mTimeEntryAdapter);

        RadialTimePickerDialogFragment radialTimePickerDialogFragment = (RadialTimePickerDialogFragment) getChildFragmentManager().findFragmentByTag(TIME_PICKER_TAG);
        if (radialTimePickerDialogFragment != null)
            radialTimePickerDialogFragment.setOnTimeSetListener(mOnTimeSetListener);
    }

    @Override
    public void onLoaderReset(Loader<DailyScheduleLoader.Data> loader) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(TIME_ENTRY_KEY, mTimeEntryAdapter.getTimeEntries());
        outState.putInt(HOUR_MINUTE_PICKER_POSITION_KEY, mHourMinutePickerPosition);
    }

    private ArrayList<TimePair> getTimePairs() {
        ArrayList<TimeEntry> timeEntries = mTimeEntryAdapter.getTimeEntries();
        Assert.assertTrue(!timeEntries.isEmpty());

        ArrayList<TimePair> timePairs = new ArrayList<>();
        for (TimeEntry timeEntry : timeEntries)
            timePairs.add(new TimePair(timeEntry.getCustomTimeId(), timeEntry.getHourMinute()));
        Assert.assertTrue(!timePairs.isEmpty());

        return timePairs;
    }

    @Override
    public void createRootTask(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Assert.assertTrue(mRootTaskId == null);

        ArrayList<TimePair> timePairs = getTimePairs();
        Assert.assertTrue(!timePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).createDailyScheduleRootTask(mData.DataId, name, timePairs);

        TickService.startService(getActivity());
    }

    @Override
    public void updateRootTask(int rootTaskId, String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        ArrayList<TimePair> timePairs = getTimePairs();
        Assert.assertTrue(!timePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).updateDailyScheduleRootTask(mData.DataId, rootTaskId, name, timePairs);

        TickService.startService(getActivity());
    }

    @Override
    public void createRootJoinTask(String name, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        Assert.assertTrue(mRootTaskId == null);

        ArrayList<TimePair> timePairs = getTimePairs();
        Assert.assertTrue(!timePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).createDailyScheduleJoinRootTask(mData.DataId, name, timePairs, joinTaskIds);

        TickService.startService(getActivity());
    }

    private class TimeEntryAdapter extends RecyclerView.Adapter<TimeEntryAdapter.TimeHolder> {
        private final ArrayList<TimeEntry> mTimeEntries;
        private final Context mContext;

        public TimeEntryAdapter(Context context) {
            Assert.assertTrue(context != null);

            mContext = context;
            mTimeEntries = new ArrayList<>();
            mTimeEntries.add(new TimeEntry(new TimePair(mHourMinute), false));
        }

        public TimeEntryAdapter(Context context, List<TimeEntry> timeEntries) {
            Assert.assertTrue(context != null);
            Assert.assertTrue(timeEntries != null);
            Assert.assertTrue(!timeEntries.isEmpty());

            mContext = context;
            mTimeEntries = new ArrayList<>(timeEntries);
        }

        public TimeEntry getTimeEntry(int position) {
            Assert.assertTrue(position >= 0);
            Assert.assertTrue(position < getItemCount());

            return mTimeEntries.get(position);
        }

        @Override
        public TimeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View dailyScheduleRow = LayoutInflater.from(mContext).inflate(R.layout.row_daily_schedule, parent, false);

            TimePickerView dailyScheduleTime = (TimePickerView) dailyScheduleRow.findViewById(R.id.daily_schedule_time);
            Assert.assertTrue(dailyScheduleTime != null);

            HashMap<Integer, TimePickerView.CustomTimeData> customTimeDatas = new HashMap<>();
            for (DailyScheduleLoader.CustomTimeData customTimeData : mData.CustomTimeDatas.values())
                customTimeDatas.put(customTimeData.Id, new TimePickerView.CustomTimeData(customTimeData.Id, customTimeData.Name));
            dailyScheduleTime.setCustomTimeDatas(customTimeDatas);

            ImageView dailyScheduleImage = (ImageView) dailyScheduleRow.findViewById(R.id.daily_schedule_image);

            return new TimeHolder(dailyScheduleRow, dailyScheduleTime, dailyScheduleImage);
        }

        @Override
        public void onBindViewHolder(final TimeHolder timeHolder, int position) {
            final TimeEntry timeEntry = mTimeEntries.get(position);
            Assert.assertTrue(timeEntry != null);

            if (timeEntry.getCustomTimeId() != null) {
                Assert.assertTrue(timeEntry.getHourMinute() == null);
                timeHolder.mDailyScheduleTime.setCustomTimeId(timeEntry.getCustomTimeId());
            } else {
                Assert.assertTrue(timeEntry.getHourMinute() != null);
                timeHolder.mDailyScheduleTime.setHourMinute(timeEntry.getHourMinute());
            }

            timeHolder.mDailyScheduleTime.setOnTimeSelectedListener(new TimePickerView.OnTimeSelectedListener() {
                @Override
                public void onCustomTimeSelected(int customTimeId) {
                    timeEntry.setTimePair(new TimePair(customTimeId));
                }

                @Override
                public void onHourMinuteSelected(HourMinute hourMinute) {
                    timeEntry.setTimePair(new TimePair(hourMinute));
                }

                @Override
                public void onHourMinuteClick() {
                    timeHolder.onHourMinuteClick();
                }
            });

            timeHolder.mDailyScheduleImage.setVisibility(timeEntry.getShowDelete() ? View.VISIBLE : View.INVISIBLE);

            timeHolder.mDailyScheduleImage.setOnClickListener(v -> {
                Assert.assertTrue(mTimeEntries.size() > 1);
                timeHolder.delete();

                if (mTimeEntries.size() == 1) {
                    mTimeEntries.get(0).setShowDelete(false);
                    notifyItemChanged(0);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mTimeEntries.size();
        }

        public void addTimeEntry() {
            int position = mTimeEntries.size();
            Assert.assertTrue(position > 0);

            if (position == 1) {
                mTimeEntries.get(0).setShowDelete(true);
                notifyItemChanged(0);
            }

            TimeEntry timeEntry = new TimeEntry(new TimePair(HourMinute.getNow()), true);
            mTimeEntries.add(position, timeEntry);

            notifyItemInserted(position);
        }

        public ArrayList<TimeEntry> getTimeEntries() {
            return mTimeEntries;
        }

        public class TimeHolder extends RecyclerView.ViewHolder {
            public final TimePickerView mDailyScheduleTime;
            public final ImageView mDailyScheduleImage;

            public TimeHolder(View dailyScheduleRow, TimePickerView dailyScheduleTime, ImageView dailyScheduleImage) {
                super(dailyScheduleRow);

                Assert.assertTrue(dailyScheduleTime != null);
                Assert.assertTrue(dailyScheduleImage != null);

                mDailyScheduleTime = dailyScheduleTime;
                mDailyScheduleImage = dailyScheduleImage;
            }

            public void onHourMinuteClick() {
                mHourMinutePickerPosition = getAdapterPosition();
                TimeEntry timeEntry = mTimeEntries.get(mHourMinutePickerPosition);
                Assert.assertTrue(timeEntry != null);

                RadialTimePickerDialogFragment radialTimePickerDialogFragment = new RadialTimePickerDialogFragment();
                HourMinute startTime = timeEntry.getHourMinute();
                radialTimePickerDialogFragment.setStartTime(startTime.getHour(), startTime.getMinute());
                radialTimePickerDialogFragment.setOnTimeSetListener(mOnTimeSetListener);
                radialTimePickerDialogFragment.show(getChildFragmentManager(), TIME_PICKER_TAG);
            }

            public void delete() {
                int position = getAdapterPosition();
                mTimeEntries.remove(position);
                notifyItemRemoved(position);
            }
        }
    }

    public static class TimeEntry implements Parcelable {
        private TimePair mTimePair;
        private boolean mShowDelete;

        public TimeEntry(TimePair timePair, boolean showDelete) {
            mTimePair = timePair;
            mShowDelete = showDelete;
        }

        public Integer getCustomTimeId() {
            return mTimePair.CustomTimeId;
        }

        public HourMinute getHourMinute() {
            return mTimePair.HourMinute;
        }

        public void setTimePair(TimePair timePair) {
            mTimePair = timePair;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeParcelable(mTimePair, 0);
            out.writeInt(mShowDelete ? 1 : 0);
        }

        public static final Parcelable.Creator<TimeEntry> CREATOR = new Parcelable.Creator<TimeEntry>() {
            public TimeEntry createFromParcel(Parcel in) {
                TimePair timePair = in.readParcelable(TimePair.class.getClassLoader());
                Assert.assertTrue(timePair != null);

                int showDeleteInt = in.readInt();
                Assert.assertTrue(showDeleteInt == 0 || showDeleteInt == 1);
                boolean showDelete = (showDeleteInt == 1);

                return new TimeEntry(timePair, showDelete);
            }

            public TimeEntry[] newArray(int size) {
                return new TimeEntry[size];
            }
        };

        public boolean getShowDelete() {
            return mShowDelete;
        }

        public void setShowDelete(boolean delete) {
            mShowDelete = delete;
        }
    }
}