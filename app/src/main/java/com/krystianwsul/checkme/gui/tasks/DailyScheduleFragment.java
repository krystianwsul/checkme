package com.krystianwsul.checkme.gui.tasks;

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
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.TimeDialogFragment;
import com.krystianwsul.checkme.loaders.DailyScheduleLoader;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class DailyScheduleFragment extends Fragment implements ScheduleFragment, LoaderManager.LoaderCallbacks<DailyScheduleLoader.Data> {
    private static final String SCHEDULE_HINT_KEY = "scheduleHint";
    private static final String ROOT_TASK_ID_KEY = "rootTaskId";

    private static final String TIME_ENTRY_KEY = "timeEntries";
    private static final String HOUR_MINUTE_PICKER_POSITION_KEY = "hourMinutePickerPosition";

    private static final String TIME_PICKER_TAG = "timePicker";
    private static final String TIME_LIST_FRAGMENT_TAG = "timeListFragment";

    private int mHourMinutePickerPosition = -1;

    private TimeEntryAdapter mTimeEntryAdapter;
    private RecyclerView mDailyScheduleTimes;

    private Bundle mSavedInstanceState;

    private HourMinute mHourMinute;

    private Integer mRootTaskId;
    private DailyScheduleLoader.Data mData;

    private FloatingActionButton mDailyScheduleFab;

    private final TimeDialogFragment.TimeDialogListener mTimeDialogListener = new TimeDialogFragment.TimeDialogListener() {
        @Override
        public void onCustomTimeSelected(int customTimeId) {
            Assert.assertTrue(mHourMinutePickerPosition != -1);
            Assert.assertTrue(mData != null);

            TimeEntry timeEntry = mTimeEntryAdapter.getTimeEntry(mHourMinutePickerPosition);
            Assert.assertTrue(timeEntry != null);

            timeEntry.mTimePairPersist.setCustomTimeId(customTimeId);
            mTimeEntryAdapter.notifyItemChanged(mHourMinutePickerPosition);

            mHourMinutePickerPosition = -1;
        }

        @Override
        public void onHourMinuteSelected() {
            Assert.assertTrue(mHourMinutePickerPosition != -1);
            Assert.assertTrue(mData != null);

            TimeEntry timeEntry = mTimeEntryAdapter.getTimeEntry(mHourMinutePickerPosition);
            Assert.assertTrue(timeEntry != null);

            RadialTimePickerDialogFragment radialTimePickerDialogFragment = new RadialTimePickerDialogFragment();
            radialTimePickerDialogFragment.setStartTime(timeEntry.mTimePairPersist.getHourMinute().getHour(), timeEntry.mTimePairPersist.getHourMinute().getMinute());
            radialTimePickerDialogFragment.setOnTimeSetListener(mOnTimeSetListener);
            radialTimePickerDialogFragment.show(getChildFragmentManager(), TIME_PICKER_TAG);
        }
    };

    private final RadialTimePickerDialogFragment.OnTimeSetListener mOnTimeSetListener = new RadialTimePickerDialogFragment.OnTimeSetListener() {
        @Override
        public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
            Assert.assertTrue(mHourMinutePickerPosition != -1);
            Assert.assertTrue(mData != null);

            TimeEntry timeEntry = mTimeEntryAdapter.getTimeEntry(mHourMinutePickerPosition);
            Assert.assertTrue(timeEntry != null);

            timeEntry.mTimePairPersist.setHourMinute(new HourMinute(hourOfDay, minute));
            mTimeEntryAdapter.notifyItemChanged(mHourMinutePickerPosition);

            mHourMinutePickerPosition = -1;
        }
    };

    public static DailyScheduleFragment newInstance() {
        return new DailyScheduleFragment();
    }

    public static DailyScheduleFragment newInstance(CreateTaskActivity.ScheduleHint scheduleHint) {
        Assert.assertTrue(scheduleHint != null);

        DailyScheduleFragment dailyScheduleFragment = new DailyScheduleFragment();

        Bundle args = new Bundle();
        args.putParcelable(SCHEDULE_HINT_KEY, scheduleHint);

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
                Assert.assertTrue(!args.containsKey(SCHEDULE_HINT_KEY));

                mRootTaskId = args.getInt(ROOT_TASK_ID_KEY, -1);
                Assert.assertTrue(mRootTaskId != -1);
            } else {
                Assert.assertTrue(args.containsKey(SCHEDULE_HINT_KEY));

                CreateTaskActivity.ScheduleHint scheduleHint = args.getParcelable(SCHEDULE_HINT_KEY);
                Assert.assertTrue(scheduleHint != null);

                mHourMinute = scheduleHint.mHourMinute;
            }
        }

        mDailyScheduleFab = (FloatingActionButton) view.findViewById(R.id.daily_schedule_fab);
        Assert.assertTrue(mDailyScheduleFab != null);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("DailyScheduleFragment.onResume");

        super.onResume();
    }

    @Override
    public Loader<DailyScheduleLoader.Data> onCreateLoader(int id, Bundle args) {
        return new DailyScheduleLoader(getActivity(), mRootTaskId);
    }

    @Override
    public void onLoadFinished(Loader<DailyScheduleLoader.Data> loader, DailyScheduleLoader.Data data) {
        mData = data;

        Bundle args = getArguments();
        if (mSavedInstanceState != null && mSavedInstanceState.containsKey(TIME_ENTRY_KEY)) {
            List<TimeEntry> timeEntries = mSavedInstanceState.getParcelableArrayList(TIME_ENTRY_KEY);
            mTimeEntryAdapter = new TimeEntryAdapter(getContext(), timeEntries);

            mHourMinutePickerPosition = mSavedInstanceState.getInt(HOUR_MINUTE_PICKER_POSITION_KEY, -2);
            Assert.assertTrue(mHourMinutePickerPosition != -2);
        } else if (args != null && args.containsKey(ROOT_TASK_ID_KEY)) {
            Assert.assertTrue(mData.ScheduleDatas != null);
            Assert.assertTrue(!mData.ScheduleDatas.isEmpty());

            boolean showDelete = (mData.ScheduleDatas.size() > 1);
            List<TimeEntry> timeEntries = Stream.of(mData.ScheduleDatas)
                    .map(scheduleData -> new TimeEntry(scheduleData.TimePair, showDelete))
                    .collect(Collectors.toList());

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

        TimeDialogFragment timeDialogFragment = (TimeDialogFragment) getChildFragmentManager().findFragmentByTag(TIME_LIST_FRAGMENT_TAG);
        if (timeDialogFragment != null)
            timeDialogFragment.setTimeDialogListener(mTimeDialogListener);

        mDailyScheduleFab.setOnClickListener(v -> {
            Assert.assertTrue(mTimeEntryAdapter != null);
            mTimeEntryAdapter.addTimeEntry();
        });
        mDailyScheduleFab.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<DailyScheduleLoader.Data> loader) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mData != null) {
            Assert.assertTrue(mTimeEntryAdapter != null);

            outState.putParcelableArrayList(TIME_ENTRY_KEY, mTimeEntryAdapter.getTimeEntries());
            outState.putInt(HOUR_MINUTE_PICKER_POSITION_KEY, mHourMinutePickerPosition);
        }
    }

    private List<TimePair> getTimePairs() {
        ArrayList<TimeEntry> timeEntries = mTimeEntryAdapter.getTimeEntries();
        Assert.assertTrue(!timeEntries.isEmpty());

        return Stream.of(timeEntries)
                .map(timeEntry -> timeEntry.mTimePairPersist.getTimePair())
                .collect(Collectors.toList());
    }

    @Override
    public boolean createRootTask(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Assert.assertTrue(mRootTaskId == null);

        if (mData == null)
            return false;

        List<TimePair> timePairs = getTimePairs();
        Assert.assertTrue(!timePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).createDailyScheduleRootTask(mData.DataId, name, timePairs);

        TickService.startService(getActivity());

        return true;
    }

    @Override
    public boolean updateRootTask(int rootTaskId, String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        if (mData == null)
            return false;

        List<TimePair> timePairs = getTimePairs();
        Assert.assertTrue(!timePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).updateDailyScheduleRootTask(mData.DataId, rootTaskId, name, timePairs);

        TickService.startService(getActivity());

        return true;
    }

    @Override
    public boolean createRootJoinTask(String name, List<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        Assert.assertTrue(mRootTaskId == null);

        if (mData == null)
            return false;

        List<TimePair> timePairs = getTimePairs();
        Assert.assertTrue(!timePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).createDailyScheduleJoinRootTask(mData.DataId, name, timePairs, joinTaskIds);

        TickService.startService(getActivity());

        return true;
    }

    private class TimeEntryAdapter extends RecyclerView.Adapter<TimeEntryAdapter.TimeHolder> {
        private final ArrayList<TimeEntry> mTimeEntries;
        private final Context mContext;

        public TimeEntryAdapter(Context context) {
            Assert.assertTrue(context != null);

            mContext = context;
            mTimeEntries = new ArrayList<>();
            if (mHourMinute != null)
                mTimeEntries.add(new TimeEntry(mHourMinute, false));
            else
                mTimeEntries.add(new TimeEntry(false));
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

            TextView dailyScheduleTime = (TextView) dailyScheduleRow.findViewById(R.id.daily_schedule_time);
            Assert.assertTrue(dailyScheduleTime != null);

            ImageView dailyScheduleImage = (ImageView) dailyScheduleRow.findViewById(R.id.daily_schedule_image);
            Assert.assertTrue(dailyScheduleImage != null);

            return new TimeHolder(dailyScheduleRow, dailyScheduleTime, dailyScheduleImage);
        }

        @Override
        public void onBindViewHolder(final TimeHolder timeHolder, int position) {
            final TimeEntry timeEntry = mTimeEntries.get(position);
            Assert.assertTrue(timeEntry != null);

            if (timeEntry.mTimePairPersist.getCustomTimeId() != null) {
                DailyScheduleLoader.CustomTimeData customTimeData = mData.CustomTimeDatas.get(timeEntry.mTimePairPersist.getCustomTimeId());
                Assert.assertTrue(customTimeData != null);

                timeHolder.mDailyScheduleTime.setText(customTimeData.Name);
            } else {
                timeHolder.mDailyScheduleTime.setText(timeEntry.mTimePairPersist.getHourMinute().toString());
            }

            timeHolder.mDailyScheduleTime.setOnClickListener(v -> timeHolder.onTimeClick());

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

            TimeEntry timeEntry;
            if (mHourMinute != null)
                timeEntry = new TimeEntry(mHourMinute, true);
            else
                timeEntry = new TimeEntry(true);
            mTimeEntries.add(position, timeEntry);
            notifyItemInserted(position);
        }

        public ArrayList<TimeEntry> getTimeEntries() {
            return mTimeEntries;
        }

        public class TimeHolder extends RecyclerView.ViewHolder {
            public final TextView mDailyScheduleTime;
            public final ImageView mDailyScheduleImage;

            public TimeHolder(View dailyScheduleRow, TextView dailyScheduleTime, ImageView dailyScheduleImage) {
                super(dailyScheduleRow);

                Assert.assertTrue(dailyScheduleTime != null);
                Assert.assertTrue(dailyScheduleImage != null);

                mDailyScheduleTime = dailyScheduleTime;
                mDailyScheduleImage = dailyScheduleImage;
            }

            public void onTimeClick() {
                Assert.assertTrue(mData != null);

                mHourMinutePickerPosition = getAdapterPosition();

                ArrayList<TimeDialogFragment.CustomTimeData> customTimeDatas = new ArrayList<>(Stream.of(mData.CustomTimeDatas.values())
                        .sortBy(customTimeData -> customTimeData.Id)
                        .map(customTimeData -> new TimeDialogFragment.CustomTimeData(customTimeData.Id, customTimeData.Name))
                        .collect(Collectors.toList()));

                TimeDialogFragment timeDialogFragment = TimeDialogFragment.newInstance(customTimeDatas);
                Assert.assertTrue(timeDialogFragment != null);

                timeDialogFragment.setTimeDialogListener(mTimeDialogListener);

                timeDialogFragment.show(getChildFragmentManager(), TIME_LIST_FRAGMENT_TAG);
            }

            public void delete() {
                int position = getAdapterPosition();
                mTimeEntries.remove(position);
                notifyItemRemoved(position);
            }
        }
    }

    public static class TimeEntry implements Parcelable {
        public final TimePairPersist mTimePairPersist;
        private boolean mShowDelete;

        private TimeEntry(TimePairPersist timePairPersist, boolean showDelete) {
            Assert.assertTrue(timePairPersist != null);

            mTimePairPersist = timePairPersist;
            mShowDelete = showDelete;
        }

        private TimeEntry(TimePair timePair, boolean showDelete) {
            Assert.assertTrue(timePair != null);

            mTimePairPersist = new TimePairPersist(timePair);
            mShowDelete = showDelete;
        }

        private TimeEntry(boolean showDelete) {
            mTimePairPersist = new TimePairPersist();
            mShowDelete = showDelete;
        }

        private TimeEntry(HourMinute hourMinute, boolean showDelete) {
            Assert.assertTrue(hourMinute != null);

            mTimePairPersist = new TimePairPersist(hourMinute);
            mShowDelete = showDelete;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeParcelable(mTimePairPersist, 0);
            out.writeInt(mShowDelete ? 1 : 0);
        }

        public static final Parcelable.Creator<TimeEntry> CREATOR = new Parcelable.Creator<TimeEntry>() {
            public TimeEntry createFromParcel(Parcel in) {
                TimePairPersist timePairPersist = in.readParcelable(TimePair.class.getClassLoader());
                Assert.assertTrue(timePairPersist != null);

                int showDeleteInt = in.readInt();
                Assert.assertTrue(showDeleteInt == 0 || showDeleteInt == 1);
                boolean showDelete = (showDeleteInt == 1);

                return new TimeEntry(timePairPersist, showDelete);
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

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean dataChanged() {
        Assert.assertTrue(mRootTaskId != null);

        if (mData == null)
            return false;

        Assert.assertTrue(mTimeEntryAdapter != null);

        Assert.assertTrue(mData.ScheduleDatas != null);

        List<TimePair> oldTimePairs = Stream.of(mData.ScheduleDatas)
                .map(scheduleData -> scheduleData.TimePair)
                .sortBy(TimePair::hashCode)
                .collect(Collectors.toList());

        List<TimePair> newTimePairs = Stream.of(mTimeEntryAdapter.getTimeEntries())
                .map(timeEntry -> timeEntry.mTimePairPersist.getTimePair())
                .sortBy(TimePair::hashCode)
                .collect(Collectors.toList());

        if (!oldTimePairs.equals(newTimePairs))
            return true;

        return false;
    }
}