package com.krystianwsul.checkme.gui.tasks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.TimeDialogFragment;
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity;
import com.krystianwsul.checkme.loaders.WeeklyScheduleLoader;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class WeeklyScheduleFragment extends Fragment implements ScheduleFragment, LoaderManager.LoaderCallbacks<WeeklyScheduleLoader.Data> {
    private static final String SCHEDULE_HINT_KEY = "scheduleHint";
    private static final String ROOT_TASK_ID_KEY = "rootTaskId";

    private static final String DATE_TIME_ENTRY_KEY = "dateTimeEntries";
    private static final String HOUR_MINUTE_PICKER_POSITION_KEY = "hourMinutePickerPosition";

    private static final String TIME_PICKER_TAG = "timePicker";
    private static final String TIME_LIST_FRAGMENT_TAG = "timeListFragment";

    private int mHourMinutePickerPosition = -1;

    private RecyclerView mDailyScheduleTimes;
    private DayOfWeekTimeEntryAdapter mDayOfWeekTimeEntryAdapter;

    private Bundle mSavedInstanceState;

    private DayOfWeek mDayOfWeek = DayOfWeek.today();
    private TimePair mTimePair = null;

    private Integer mRootTaskId;
    private WeeklyScheduleLoader.Data mData;

    private FloatingActionButton mWeeklyScheduleFab;

    private List<DayOfWeekTimeEntry> mDayOfWeekTimeEntries;

    private boolean mFirst = true;

    private final TimeDialogFragment.TimeDialogListener mTimeDialogListener = new TimeDialogFragment.TimeDialogListener() {
        @Override
        public void onCustomTimeSelected(int customTimeId) {
            Assert.assertTrue(mHourMinutePickerPosition != -1);
            Assert.assertTrue(mData != null);

            DayOfWeekTimeEntry dayOfWeekTimeEntry = mDayOfWeekTimeEntries.get(mHourMinutePickerPosition);
            Assert.assertTrue(dayOfWeekTimeEntry != null);

            dayOfWeekTimeEntry.mTimePairPersist.setCustomTimeId(customTimeId);
            mDayOfWeekTimeEntryAdapter.notifyItemChanged(mHourMinutePickerPosition);

            mHourMinutePickerPosition = -1;
        }

        @Override
        public void onOtherSelected() {
            Assert.assertTrue(mHourMinutePickerPosition != -1);
            Assert.assertTrue(mData != null);

            DayOfWeekTimeEntry dayOfWeekTimeEntry = mDayOfWeekTimeEntries.get(mHourMinutePickerPosition);
            Assert.assertTrue(dayOfWeekTimeEntry != null);

            RadialTimePickerDialogFragment radialTimePickerDialogFragment = new RadialTimePickerDialogFragment();
            radialTimePickerDialogFragment.setStartTime(dayOfWeekTimeEntry.mTimePairPersist.getHourMinute().getHour(), dayOfWeekTimeEntry.mTimePairPersist.getHourMinute().getMinute());
            radialTimePickerDialogFragment.setOnTimeSetListener(mOnTimeSetListener);
            radialTimePickerDialogFragment.show(getChildFragmentManager(), TIME_PICKER_TAG);
        }

        @Override
        public void onAddSelected() {
            startActivityForResult(ShowCustomTimeActivity.getCreateIntent(getActivity()), ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE);
        }
    };

    private final RadialTimePickerDialogFragment.OnTimeSetListener mOnTimeSetListener = new RadialTimePickerDialogFragment.OnTimeSetListener() {
        @Override
        public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
            Assert.assertTrue(mHourMinutePickerPosition != -1);
            Assert.assertTrue(mData != null);

            DayOfWeekTimeEntry dayOfWeekTimeEntry = mDayOfWeekTimeEntries.get(mHourMinutePickerPosition);
            Assert.assertTrue(dayOfWeekTimeEntry != null);

            dayOfWeekTimeEntry.mTimePairPersist.setHourMinute(new HourMinute(hourOfDay, minute));
            mDayOfWeekTimeEntryAdapter.notifyItemChanged(mHourMinutePickerPosition);

            mHourMinutePickerPosition = -1;
        }
    };

    public static WeeklyScheduleFragment newInstance() {
        return new WeeklyScheduleFragment();
    }

    public static WeeklyScheduleFragment newInstance(CreateTaskActivity.ScheduleHint scheduleHint) {
        Assert.assertTrue(scheduleHint != null);

        WeeklyScheduleFragment weeklyScheduleFragment = new WeeklyScheduleFragment();

        Bundle args = new Bundle();
        args.putParcelable(SCHEDULE_HINT_KEY, scheduleHint);
        weeklyScheduleFragment.setArguments(args);

        return weeklyScheduleFragment;
    }

    public static WeeklyScheduleFragment newInstance(int rootTaskId) {
        WeeklyScheduleFragment weeklyScheduleFragment = new WeeklyScheduleFragment();

        Bundle args = new Bundle();
        args.putInt(ROOT_TASK_ID_KEY, rootTaskId);

        weeklyScheduleFragment.setArguments(args);
        return weeklyScheduleFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_weekly_schedule, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSavedInstanceState = savedInstanceState;

        View view = getView();
        Assert.assertTrue(view != null);

        mDailyScheduleTimes = (RecyclerView) view.findViewById(R.id.weekly_schedule_datetimes);
        mDailyScheduleTimes.setLayoutManager(new LinearLayoutManager(getContext()));

        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(ROOT_TASK_ID_KEY)) {
                Assert.assertTrue(!args.containsKey(SCHEDULE_HINT_KEY));

                mRootTaskId = args.getInt(ROOT_TASK_ID_KEY, -1);
                Assert.assertTrue(mRootTaskId != -1);
            } else {
                Assert.assertTrue(args.containsKey(SCHEDULE_HINT_KEY));

                CreateTaskActivity.ScheduleHint scheduleHint = args.getParcelable(SCHEDULE_HINT_KEY);
                Assert.assertTrue(scheduleHint != null);

                mDayOfWeek = scheduleHint.mDate.getDayOfWeek();
                Assert.assertTrue(mDayOfWeek != null);

                mTimePair = scheduleHint.mTimePair;
            }
        }

        if (mSavedInstanceState != null && mSavedInstanceState.containsKey(DATE_TIME_ENTRY_KEY)) {
            mDayOfWeekTimeEntries = mSavedInstanceState.getParcelableArrayList(DATE_TIME_ENTRY_KEY);

            mHourMinutePickerPosition = mSavedInstanceState.getInt(HOUR_MINUTE_PICKER_POSITION_KEY, -2);
            Assert.assertTrue(mHourMinutePickerPosition != -2);
        } else if (args != null && args.containsKey(ROOT_TASK_ID_KEY)) {
            mHourMinutePickerPosition = -1;
        } else {
            mDayOfWeekTimeEntries = new ArrayList<>();
            if (mTimePair != null)
                mDayOfWeekTimeEntries.add(new DayOfWeekTimeEntry(mDayOfWeek, mTimePair, false));
            else
                mDayOfWeekTimeEntries.add(new DayOfWeekTimeEntry(mDayOfWeek, false));

            mHourMinutePickerPosition = -1;
        }

        mWeeklyScheduleFab = (FloatingActionButton) view.findViewById(R.id.weekly_schedule_fab);
        Assert.assertTrue(mWeeklyScheduleFab != null);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("WeeklyScheduleFragment.onResume");

        super.onResume();
    }

    @Override
    public Loader<WeeklyScheduleLoader.Data> onCreateLoader(int id, Bundle args) {
        return new WeeklyScheduleLoader(getActivity(), mRootTaskId);
    }

    @Override
    public void onLoadFinished(Loader<WeeklyScheduleLoader.Data> loader, WeeklyScheduleLoader.Data data) {
        mData = data;

        if (mFirst && (mSavedInstanceState == null || !mSavedInstanceState.containsKey(DATE_TIME_ENTRY_KEY)) && mData.ScheduleDatas != null) {
            Assert.assertTrue(mDayOfWeekTimeEntries == null);

            mFirst = false;

            mDayOfWeekTimeEntries = new ArrayList<>();
            boolean showDelete = (mData.ScheduleDatas.size() > 1);
            for (WeeklyScheduleLoader.ScheduleData scheduleData : mData.ScheduleDatas)
                mDayOfWeekTimeEntries.add(new DayOfWeekTimeEntry(scheduleData.DayOfWeek, scheduleData.TimePair, showDelete));
        }

        mDayOfWeekTimeEntryAdapter = new DayOfWeekTimeEntryAdapter(getContext());
        mDailyScheduleTimes.setAdapter(mDayOfWeekTimeEntryAdapter);

        RadialTimePickerDialogFragment radialTimePickerDialogFragment = (RadialTimePickerDialogFragment) getChildFragmentManager().findFragmentByTag(TIME_PICKER_TAG);
        if (radialTimePickerDialogFragment != null)
            radialTimePickerDialogFragment.setOnTimeSetListener(mOnTimeSetListener);

        TimeDialogFragment timeDialogFragment = (TimeDialogFragment) getChildFragmentManager().findFragmentByTag(TIME_LIST_FRAGMENT_TAG);
        if (timeDialogFragment != null)
            timeDialogFragment.setTimeDialogListener(mTimeDialogListener);

        mWeeklyScheduleFab.setOnClickListener(v -> {
            Assert.assertTrue(mDayOfWeekTimeEntryAdapter != null);
            mDayOfWeekTimeEntryAdapter.addDayOfWeekTimeEntry();
        });
        mWeeklyScheduleFab.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<WeeklyScheduleLoader.Data> loader) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mData != null) {
            Assert.assertTrue(mDayOfWeekTimeEntryAdapter != null);

            outState.putParcelableArrayList(DATE_TIME_ENTRY_KEY, new ArrayList<>(mDayOfWeekTimeEntries));
            outState.putInt(HOUR_MINUTE_PICKER_POSITION_KEY, mHourMinutePickerPosition);
        }
    }

    private ArrayList<Pair<DayOfWeek, TimePair>> getDayOfWeekTimePairs() {
        Assert.assertTrue(!mDayOfWeekTimeEntries.isEmpty());

        ArrayList<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs = new ArrayList<>();
        for (DayOfWeekTimeEntry dayOfWeekTimeEntry : mDayOfWeekTimeEntries)
            dayOfWeekTimePairs.add(new Pair<>(dayOfWeekTimeEntry.mDayOfWeek, dayOfWeekTimeEntry.mTimePairPersist.getTimePair()));
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        return dayOfWeekTimePairs;
    }

    @Override
    public boolean createRootTask(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        if (mData == null)
            return false;

        ArrayList<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs = getDayOfWeekTimePairs();
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).createWeeklyScheduleRootTask(mData.DataId, name, dayOfWeekTimePairs);

        TickService.startService(getActivity());

        return true;
    }

    @Override
    public boolean updateRootTask(int rootTaskId, String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        if (mData == null)
            return false;

        ArrayList<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs = getDayOfWeekTimePairs();
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).updateWeeklyScheduleTask(mData.DataId, rootTaskId, name, dayOfWeekTimePairs);

        TickService.startService(getActivity());

        return true;
    }

    @Override
    public boolean createRootJoinTask(String name, List<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        if (mData == null)
            return false;

        ArrayList<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs = getDayOfWeekTimePairs();
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).createWeeklyScheduleJoinRootTask(mData.DataId, name, dayOfWeekTimePairs, joinTaskIds);

        TickService.startService(getActivity());

        return true;
    }

    private class DayOfWeekTimeEntryAdapter extends RecyclerView.Adapter<DayOfWeekTimeEntryAdapter.DayOfWeekTimeHolder> {
        private final Context mContext;

        public DayOfWeekTimeEntryAdapter(Context context) {
            Assert.assertTrue(context != null);

            mContext = context;
        }

        @Override
        public DayOfWeekTimeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View weeklyScheduleRow = LayoutInflater.from(mContext).inflate(R.layout.row_weekly_schedule, parent, false);

            Spinner weeklyScheduleDay = (Spinner) weeklyScheduleRow.findViewById(R.id.weekly_schedule_day);
            Assert.assertTrue(weeklyScheduleDay != null);

            TextView weeklyScheduleTime = (TextView) weeklyScheduleRow.findViewById(R.id.weekly_schedule_time);
            Assert.assertTrue(weeklyScheduleTime != null);

            ImageView weeklyScheduleImage = (ImageView) weeklyScheduleRow.findViewById(R.id.weekly_schedule_image);
            Assert.assertTrue(weeklyScheduleImage != null);

            return new DayOfWeekTimeHolder(weeklyScheduleRow, weeklyScheduleDay, weeklyScheduleTime, weeklyScheduleImage);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(final DayOfWeekTimeHolder dayOfWeekTimeHolder, int position) {
            final DayOfWeekTimeEntry dayOfWeekTimeEntry = mDayOfWeekTimeEntries.get(position);
            Assert.assertTrue(dayOfWeekTimeEntry != null);

            final ArrayAdapter<DayOfWeek> dayOfWeekAdapter = new ArrayAdapter<>(getContext(), R.layout.spinner_no_padding, DayOfWeek.values());
            dayOfWeekAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            dayOfWeekTimeHolder.mWeeklyScheduleDay.setAdapter(dayOfWeekAdapter);
            dayOfWeekTimeHolder.mWeeklyScheduleDay.setSelection(dayOfWeekAdapter.getPosition(dayOfWeekTimeEntry.mDayOfWeek));

            dayOfWeekTimeHolder.mWeeklyScheduleDay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    DayOfWeek dayOfWeek = dayOfWeekAdapter.getItem(position);
                    Assert.assertTrue(dayOfWeek != null);

                    dayOfWeekTimeHolder.onDayOfWeekSelected(dayOfWeek);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            if (dayOfWeekTimeEntry.mTimePairPersist.getCustomTimeId() != null) {
                WeeklyScheduleLoader.CustomTimeData customTimeData = mData.CustomTimeDatas.get(dayOfWeekTimeEntry.mTimePairPersist.getCustomTimeId());
                Assert.assertTrue(customTimeData != null);

                dayOfWeekTimeHolder.mWeeklyScheduleTime.setText(customTimeData.Name + " (" + customTimeData.HourMinutes.get(dayOfWeekTimeEntry.mDayOfWeek) + ")");
            } else {
                dayOfWeekTimeHolder.mWeeklyScheduleTime.setText(dayOfWeekTimeEntry.mTimePairPersist.getHourMinute().toString());
            }

            dayOfWeekTimeHolder.mWeeklyScheduleTime.setOnClickListener(v -> dayOfWeekTimeHolder.onTimeClick());

            dayOfWeekTimeHolder.mWeeklyScheduleImage.setVisibility(dayOfWeekTimeEntry.getShowDelete() ? View.VISIBLE : View.INVISIBLE);

            dayOfWeekTimeHolder.mWeeklyScheduleImage.setOnClickListener(v -> {
                Assert.assertTrue(mDayOfWeekTimeEntries.size() > 1);
                dayOfWeekTimeHolder.delete();

                if (mDayOfWeekTimeEntries.size() == 1) {
                    mDayOfWeekTimeEntries.get(0).setShowDelete(false);
                    notifyItemChanged(0);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mDayOfWeekTimeEntries.size();
        }

        public void addDayOfWeekTimeEntry() {
            int position = mDayOfWeekTimeEntries.size();
            Assert.assertTrue(position > 0);

            if (position == 1) {
                mDayOfWeekTimeEntries.get(0).setShowDelete(true);
                notifyItemChanged(0);
            }

            DayOfWeekTimeEntry dayOfWeekTimeEntry;
            if (mTimePair != null)
                dayOfWeekTimeEntry = new DayOfWeekTimeEntry(mDayOfWeek, mTimePair, true);
            else
                dayOfWeekTimeEntry = new DayOfWeekTimeEntry(mDayOfWeek, true);
            mDayOfWeekTimeEntries.add(position, dayOfWeekTimeEntry);
            notifyItemInserted(position);
        }

        public class DayOfWeekTimeHolder extends RecyclerView.ViewHolder {
            public final Spinner mWeeklyScheduleDay;
            public final TextView mWeeklyScheduleTime;
            public final ImageView mWeeklyScheduleImage;

            public DayOfWeekTimeHolder(View weeklyScheduleRow, Spinner weeklyScheduleDay, TextView weeklyScheduleTime, ImageView weeklyScheduleImage) {
                super(weeklyScheduleRow);

                Assert.assertTrue(weeklyScheduleDay != null);
                Assert.assertTrue(weeklyScheduleTime != null);
                Assert.assertTrue(weeklyScheduleImage != null);

                mWeeklyScheduleDay = weeklyScheduleDay;
                mWeeklyScheduleTime = weeklyScheduleTime;
                mWeeklyScheduleImage = weeklyScheduleImage;
            }

            @SuppressLint("SetTextI18n")
            public void onDayOfWeekSelected(DayOfWeek dayOfWeek) {
                Assert.assertTrue(dayOfWeek != null);

                DayOfWeekTimeEntry dayOfWeekTimeEntry = mDayOfWeekTimeEntries.get(getAdapterPosition());
                Assert.assertTrue(dayOfWeekTimeEntry != null);

                dayOfWeekTimeEntry.mDayOfWeek = dayOfWeek;

                if (dayOfWeekTimeEntry.mTimePairPersist.getCustomTimeId() != null) {
                    WeeklyScheduleLoader.CustomTimeData customTimeData = mData.CustomTimeDatas.get(dayOfWeekTimeEntry.mTimePairPersist.getCustomTimeId());
                    Assert.assertTrue(customTimeData != null);

                    mWeeklyScheduleTime.setText(customTimeData.Name + " (" + customTimeData.HourMinutes.get(dayOfWeekTimeEntry.mDayOfWeek) + ")");
                } else {
                    mWeeklyScheduleTime.setText(dayOfWeekTimeEntry.mTimePairPersist.getHourMinute().toString());
                }
            }

            public void onTimeClick() {
                Assert.assertTrue(mData != null);

                mHourMinutePickerPosition = getAdapterPosition();

                DayOfWeekTimeEntry dayOfWeekTimeEntry = mDayOfWeekTimeEntries.get(mHourMinutePickerPosition);
                Assert.assertTrue(dayOfWeekTimeEntry != null);

                ArrayList<TimeDialogFragment.CustomTimeData> customTimeDatas = new ArrayList<>(Stream.of(mData.CustomTimeDatas.values())
                        .sortBy(customTimeData -> customTimeData.HourMinutes.get(dayOfWeekTimeEntry.mDayOfWeek))
                        .map(customTimeData -> new TimeDialogFragment.CustomTimeData(customTimeData.Id, customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDayOfWeek) + ")"))
                        .collect(Collectors.toList()));

                TimeDialogFragment timeDialogFragment = TimeDialogFragment.newInstance(customTimeDatas);
                Assert.assertTrue(timeDialogFragment != null);

                timeDialogFragment.setTimeDialogListener(mTimeDialogListener);

                timeDialogFragment.show(getChildFragmentManager(), TIME_LIST_FRAGMENT_TAG);
            }

            public void delete() {
                int position = getAdapterPosition();
                mDayOfWeekTimeEntries.remove(position);
                notifyItemRemoved(position);
            }
        }
    }

    public static class DayOfWeekTimeEntry implements Parcelable {
        public DayOfWeek mDayOfWeek;
        public final TimePairPersist mTimePairPersist;
        private boolean mShowDelete = false;

        public DayOfWeekTimeEntry(DayOfWeek dayOfWeek, boolean showDelete) {
            Assert.assertTrue(dayOfWeek != null);

            mDayOfWeek = dayOfWeek;
            mTimePairPersist = new TimePairPersist();
            mShowDelete = showDelete;
        }

        public DayOfWeekTimeEntry(DayOfWeek dayOfWeek, HourMinute hourMinute, boolean showDelete) {
            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(hourMinute != null);

            mDayOfWeek = dayOfWeek;
            mTimePairPersist = new TimePairPersist(hourMinute);
            mShowDelete = showDelete;
        }

        private DayOfWeekTimeEntry(DayOfWeek dayOfWeek, TimePair timePair, boolean showDelete) {
            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(timePair != null);

            mDayOfWeek = dayOfWeek;
            mTimePairPersist = new TimePairPersist(timePair);
            mShowDelete = showDelete;
        }

        private DayOfWeekTimeEntry(DayOfWeek dayOfWeek, TimePairPersist timePairPersist, boolean showDelete) {
            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(timePairPersist != null);

            mDayOfWeek = dayOfWeek;
            mTimePairPersist = timePairPersist;
            mShowDelete = showDelete;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeSerializable(mDayOfWeek);
            out.writeParcelable(mTimePairPersist, 0);
            out.writeInt(mShowDelete ? 1 : 0);
        }

        public static final Parcelable.Creator<DayOfWeekTimeEntry> CREATOR = new Creator<DayOfWeekTimeEntry>() {
            public DayOfWeekTimeEntry createFromParcel(Parcel in) {
                DayOfWeek dayOfWeek = (DayOfWeek) in.readSerializable();
                TimePairPersist timePairPersist = in.readParcelable(TimePairPersist.class.getClassLoader());
                int showDeleteInt = in.readInt();
                Assert.assertTrue(showDeleteInt == 0 || showDeleteInt == 1);
                boolean showDelete = (showDeleteInt == 1);

                return new DayOfWeekTimeEntry(dayOfWeek, timePairPersist, showDelete);
            }

            public DayOfWeekTimeEntry[] newArray(int size) {
                return new DayOfWeekTimeEntry[size];
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

        Assert.assertTrue(mDayOfWeekTimeEntryAdapter != null);

        Assert.assertTrue(mData.ScheduleDatas != null);

        List<Pair<DayOfWeek, TimePair>> oldDayOfWeekTimePairs = Stream.of(mData.ScheduleDatas)
                .map(scheduleData -> new Pair<>(scheduleData.DayOfWeek, scheduleData.TimePair))
                .sortBy(Pair::hashCode)
                .collect(Collectors.toList());

        List<Pair<DayOfWeek, TimePair>> newDayOfWeekTimePairs = Stream.of(mDayOfWeekTimeEntries)
                .map(dayOfWeekTimeEntry -> new Pair<>(dayOfWeekTimeEntry.mDayOfWeek, dayOfWeekTimeEntry.mTimePairPersist.getTimePair()))
                .sortBy(Pair::hashCode)
                .collect(Collectors.toList());

        if (!oldDayOfWeekTimePairs.equals(newDayOfWeekTimePairs))
            return true;

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Assert.assertTrue(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE);
        Assert.assertTrue(resultCode >= 0);
        Assert.assertTrue(data == null);

        Assert.assertTrue(mHourMinutePickerPosition >= 0);

        if (resultCode > 0) {
            DayOfWeekTimeEntry dayOfWeekTimeEntry = mDayOfWeekTimeEntries.get(mHourMinutePickerPosition);
            Assert.assertTrue(dayOfWeekTimeEntry != null);

            dayOfWeekTimeEntry.mTimePairPersist.setCustomTimeId(resultCode);
        }

        mHourMinutePickerPosition = -1;
    }
}
