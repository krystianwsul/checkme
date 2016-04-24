package com.example.krystianwsul.organizator.gui.tasks;

import android.content.Context;
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

import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.loaders.WeeklyScheduleLoader;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimePair;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WeeklyScheduleFragment extends Fragment implements ScheduleFragment, LoaderManager.LoaderCallbacks<WeeklyScheduleLoader.Data> {
    private static final String DAY_OF_WEEK_KEY = "dayOfWeek";
    private static final String HOUR_MINUTE_KEY = "hourMinute";
    private static final String ROOT_TASK_ID_KEY = "rootTaskId";

    private static final String DATE_TIME_ENTRY_KEY = "dateTimeEntries";
    private static final String HOUR_MINUTE_PICKER_POSITION_KEY = "hourMinutePickerPosition";

    private static final String TIME_PICKER_TAG = "timePicker";

    private int mHourMinutePickerPosition = -1;

    private RecyclerView mDailyScheduleTimes;
    private DayOfWeekTimeEntryAdapter mDayOfWeekTimeEntryAdapter;

    private Bundle mSavedInstanceState;

    private DayOfWeek mDayOfWeek = DayOfWeek.today();
    private HourMinute mHourMinute = HourMinute.getNow();

    private Integer mRootTaskId;
    private WeeklyScheduleLoader.Data mData;

    private final RadialTimePickerDialogFragment.OnTimeSetListener mOnTimeSetListener = new RadialTimePickerDialogFragment.OnTimeSetListener() {
        @Override
        public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
            Assert.assertTrue(mHourMinutePickerPosition != -1);

            DayOfWeekTimeEntry dayOfWeekTimeEntry = mDayOfWeekTimeEntryAdapter.getDateTimeEntry(mHourMinutePickerPosition);

            dayOfWeekTimeEntry.setTimePair(new TimePair(new HourMinute(hourOfDay, minute)));
            mDayOfWeekTimeEntryAdapter.notifyItemChanged(mHourMinutePickerPosition);

            mHourMinutePickerPosition = -1;
        }
    };

    public static WeeklyScheduleFragment newInstance() {
        return new WeeklyScheduleFragment();
    }

    public static WeeklyScheduleFragment newInstance(DayOfWeek dayOfWeek) {
        Assert.assertTrue(dayOfWeek != null);

        WeeklyScheduleFragment weeklyScheduleFragment = new WeeklyScheduleFragment();

        Bundle args = new Bundle();
        args.putSerializable(DAY_OF_WEEK_KEY, dayOfWeek);
        weeklyScheduleFragment.setArguments(args);

        return weeklyScheduleFragment;
    }

    public static WeeklyScheduleFragment newInstance(DayOfWeek dayOfWeek, HourMinute hourMinute) {
        Assert.assertTrue(dayOfWeek != null);
        Assert.assertTrue(hourMinute != null);

        WeeklyScheduleFragment weeklyScheduleFragment = new WeeklyScheduleFragment();

        Bundle args = new Bundle();
        args.putSerializable(DAY_OF_WEEK_KEY, dayOfWeek);
        args.putParcelable(HOUR_MINUTE_KEY, hourMinute);
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
                Assert.assertTrue(!args.containsKey(DAY_OF_WEEK_KEY));

                mRootTaskId = args.getInt(ROOT_TASK_ID_KEY, -1);
                Assert.assertTrue(mRootTaskId != -1);
            } else {
                Assert.assertTrue(args.containsKey(DAY_OF_WEEK_KEY));

                mDayOfWeek = (DayOfWeek) args.getSerializable(DAY_OF_WEEK_KEY);
                Assert.assertTrue(mDayOfWeek != null);

                if (args.containsKey(HOUR_MINUTE_KEY)) {
                    mHourMinute = args.getParcelable(HOUR_MINUTE_KEY);
                    Assert.assertTrue(mHourMinute != null);
                }
            }
        }

        FloatingActionButton weeklyScheduleFab = (FloatingActionButton) view.findViewById(R.id.weekly_schedule_fab);
        Assert.assertTrue(weeklyScheduleFab != null);

        weeklyScheduleFab.setOnClickListener(v -> {
            Assert.assertTrue(mDayOfWeekTimeEntryAdapter != null);
            mDayOfWeekTimeEntryAdapter.addDayOfWeekTimeEntry();
        });

        ((CreateRootTaskActivity) getActivity()).setTimeValid(true);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<WeeklyScheduleLoader.Data> onCreateLoader(int id, Bundle args) {
        return new WeeklyScheduleLoader(getActivity(), mRootTaskId);
    }

    @Override
    public void onLoadFinished(Loader<WeeklyScheduleLoader.Data> loader, WeeklyScheduleLoader.Data data) {
        mData = data;

        Bundle args = getArguments();
        if (mSavedInstanceState != null) {
            List<DayOfWeekTimeEntry> dateTimeEntries = mSavedInstanceState.getParcelableArrayList(DATE_TIME_ENTRY_KEY);
            mDayOfWeekTimeEntryAdapter = new DayOfWeekTimeEntryAdapter(getContext(), dateTimeEntries);

            mHourMinutePickerPosition = mSavedInstanceState.getInt(HOUR_MINUTE_PICKER_POSITION_KEY, -2);
            Assert.assertTrue(mHourMinutePickerPosition != -2);
        } else if (args != null && args.containsKey(ROOT_TASK_ID_KEY)) {
            int rootTaskId = args.getInt(ROOT_TASK_ID_KEY, -1);
            Assert.assertTrue(rootTaskId != -1);

            ArrayList<DayOfWeekTimeEntry> dayOfWeekTimeEntries = new ArrayList<>();
            boolean showDelete = (mData.ScheduleDatas.size() > 1);
            for (WeeklyScheduleLoader.ScheduleData scheduleData : mData.ScheduleDatas)
                dayOfWeekTimeEntries.add(new DayOfWeekTimeEntry(scheduleData.DayOfWeek, scheduleData.TimePair, showDelete));

            mDayOfWeekTimeEntryAdapter = new DayOfWeekTimeEntryAdapter(getContext(), dayOfWeekTimeEntries);

            mHourMinutePickerPosition = -1;
        } else {
            mDayOfWeekTimeEntryAdapter = new DayOfWeekTimeEntryAdapter(getContext());
            mHourMinutePickerPosition = -1;
        }
        mDailyScheduleTimes.setAdapter(mDayOfWeekTimeEntryAdapter);

        RadialTimePickerDialogFragment radialTimePickerDialogFragment = (RadialTimePickerDialogFragment) getChildFragmentManager().findFragmentByTag(TIME_PICKER_TAG);
        if (radialTimePickerDialogFragment != null)
            radialTimePickerDialogFragment.setOnTimeSetListener(mOnTimeSetListener);
    }

    @Override
    public void onLoaderReset(Loader<WeeklyScheduleLoader.Data> loader) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(DATE_TIME_ENTRY_KEY, mDayOfWeekTimeEntryAdapter.getDayOfWeekTimeEntries());
        outState.putInt(HOUR_MINUTE_PICKER_POSITION_KEY, mHourMinutePickerPosition);
    }

    private ArrayList<Pair<DayOfWeek, TimePair>> getDayOfWeekTimePairs() {
        Assert.assertTrue(!mDayOfWeekTimeEntryAdapter.getDayOfWeekTimeEntries().isEmpty());

        ArrayList<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs = new ArrayList<>();
        for (DayOfWeekTimeEntry dayOfWeekTimeEntry : mDayOfWeekTimeEntryAdapter.getDayOfWeekTimeEntries())
            dayOfWeekTimePairs.add(new Pair<>(dayOfWeekTimeEntry.getDayOfWeek(), new TimePair(dayOfWeekTimeEntry.getCustomTimeId(), dayOfWeekTimeEntry.getHourMinute())));
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        return dayOfWeekTimePairs;
    }

    @Override
    public void createRootTask(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        ArrayList<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs = getDayOfWeekTimePairs();
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).createWeeklyScheduleRootTask(mData.DataId, name, dayOfWeekTimePairs);

        TickService.startService(getActivity());
    }

    @Override
    public void updateRootTask(int rootTaskId, String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        ArrayList<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs = getDayOfWeekTimePairs();
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).updateWeeklyScheduleRootTask(mData.DataId, rootTaskId, name, dayOfWeekTimePairs);

        TickService.startService(getActivity());
    }

    @Override
    public void createRootJoinTask(String name, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        ArrayList<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs = getDayOfWeekTimePairs();
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).createWeeklyScheduleJoinRootTask(mData.DataId, name, dayOfWeekTimePairs, joinTaskIds);

        TickService.startService(getActivity());
    }

    private class DayOfWeekTimeEntryAdapter extends RecyclerView.Adapter<DayOfWeekTimeEntryAdapter.DayOfWeekTimeHolder> {
        private final ArrayList<DayOfWeekTimeEntry> mDateTimeEntries;
        private final Context mContext;

        public DayOfWeekTimeEntryAdapter(Context context) {
            Assert.assertTrue(context != null);

            mContext = context;
            mDateTimeEntries = new ArrayList<>();
            mDateTimeEntries.add(new DayOfWeekTimeEntry(mDayOfWeek, new TimePair(mHourMinute), false));
        }

        public DayOfWeekTimeEntryAdapter(Context context, List<DayOfWeekTimeEntry> dateTimeEntries) {
            Assert.assertTrue(context != null);
            Assert.assertTrue(dateTimeEntries != null);
            Assert.assertTrue(!dateTimeEntries.isEmpty());

            mContext = context;
            mDateTimeEntries = new ArrayList<>(dateTimeEntries);
        }

        public DayOfWeekTimeEntry getDateTimeEntry(int position) {
            Assert.assertTrue(position >= 0);
            Assert.assertTrue(position < getItemCount());

            return mDateTimeEntries.get(position);
        }

        @Override
        public DayOfWeekTimeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View weeklyScheduleRow = LayoutInflater.from(mContext).inflate(R.layout.row_weekly_schedule, parent, false);

            Spinner weeklyScheduleDay = (Spinner) weeklyScheduleRow.findViewById(R.id.weekly_schedule_day);

            TimePickerView weeklyScheduleTime = (TimePickerView) weeklyScheduleRow.findViewById(R.id.weekly_schedule_time);
            Assert.assertTrue(weeklyScheduleTime != null);

            HashMap<Integer, TimePickerView.CustomTimeData> customTimeDatas = new HashMap<>();
            for (WeeklyScheduleLoader.CustomTimeData customTimeData : mData.CustomTimeDatas.values())
                customTimeDatas.put(customTimeData.Id, new TimePickerView.CustomTimeData(customTimeData.Id, customTimeData.Name));
            weeklyScheduleTime.setCustomTimeDatas(customTimeDatas);

            ImageView weeklyScheduleImage = (ImageView) weeklyScheduleRow.findViewById(R.id.weekly_schedule_image);

            return new DayOfWeekTimeHolder(weeklyScheduleRow, weeklyScheduleDay, weeklyScheduleTime, weeklyScheduleImage);
        }

        @Override
        public void onBindViewHolder(final DayOfWeekTimeHolder dayOfWeekTimeHolder, int position) {
            final DayOfWeekTimeEntry dayOfWeekTimeEntry = mDateTimeEntries.get(position);
            Assert.assertTrue(dayOfWeekTimeEntry != null);

            final ArrayAdapter<DayOfWeek> dayOfWeekAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, DayOfWeek.values());
            dayOfWeekAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            dayOfWeekTimeHolder.mWeeklyScheduleDay.setAdapter(dayOfWeekAdapter);
            dayOfWeekTimeHolder.mWeeklyScheduleDay.setSelection(dayOfWeekAdapter.getPosition(dayOfWeekTimeEntry.getDayOfWeek()));

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

            if (dayOfWeekTimeEntry.getCustomTimeId() != null) {
                Assert.assertTrue(dayOfWeekTimeEntry.getHourMinute() == null);
                dayOfWeekTimeHolder.mWeeklyScheduleTime.setCustomTimeId(dayOfWeekTimeEntry.getCustomTimeId());
            } else {
                Assert.assertTrue(dayOfWeekTimeEntry.getHourMinute() != null);
                dayOfWeekTimeHolder.mWeeklyScheduleTime.setHourMinute(dayOfWeekTimeEntry.getHourMinute());
            }

            dayOfWeekTimeHolder.mWeeklyScheduleTime.setOnTimeSelectedListener(new TimePickerView.OnTimeSelectedListener() {
                @Override
                public void onCustomTimeSelected(int customTimeId) {
                    dayOfWeekTimeEntry.setTimePair(new TimePair(customTimeId));
                }

                @Override
                public void onHourMinuteSelected(HourMinute hourMinute) {
                    Assert.assertTrue(hourMinute != null);
                    dayOfWeekTimeEntry.setTimePair(new TimePair(hourMinute));
                }

                @Override
                public void onHourMinuteClick() {
                    dayOfWeekTimeHolder.onHourMinuteClick();
                }
            });

            dayOfWeekTimeHolder.mWeeklyScheduleImage.setVisibility(dayOfWeekTimeEntry.getShowDelete() ? View.VISIBLE : View.INVISIBLE);

            dayOfWeekTimeHolder.mWeeklyScheduleImage.setOnClickListener(v -> {
                Assert.assertTrue(mDateTimeEntries.size() > 1);
                dayOfWeekTimeHolder.delete();

                if (mDateTimeEntries.size() == 1) {
                    mDateTimeEntries.get(0).setShowDelete(false);
                    notifyItemChanged(0);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mDateTimeEntries.size();
        }

        public void addDayOfWeekTimeEntry() {
            int position = mDateTimeEntries.size();
            Assert.assertTrue(position > 0);

            if (position == 1) {
                mDateTimeEntries.get(0).setShowDelete(true);
                notifyItemChanged(0);
            }

            DayOfWeekTimeEntry dayOfWeekTimeEntry = new DayOfWeekTimeEntry(DayOfWeek.today(), new TimePair(HourMinute.getNow()), true);
            mDateTimeEntries.add(position, dayOfWeekTimeEntry);
            notifyItemInserted(position);
        }

        public ArrayList<DayOfWeekTimeEntry> getDayOfWeekTimeEntries() {
            return mDateTimeEntries;
        }

        public class DayOfWeekTimeHolder extends RecyclerView.ViewHolder {
            public final Spinner mWeeklyScheduleDay;
            public final TimePickerView mWeeklyScheduleTime;
            public final ImageView mWeeklyScheduleImage;

            public DayOfWeekTimeHolder(View weeklyScheduleRow, Spinner weeklyScheduleDay, TimePickerView weeklyScheduleTime, ImageView weeklyScheduleImage) {
                super(weeklyScheduleRow);

                Assert.assertTrue(weeklyScheduleDay != null);
                Assert.assertTrue(weeklyScheduleTime != null);
                Assert.assertTrue(weeklyScheduleImage != null);

                mWeeklyScheduleDay = weeklyScheduleDay;
                mWeeklyScheduleTime = weeklyScheduleTime;
                mWeeklyScheduleImage = weeklyScheduleImage;
            }

            public void onDayOfWeekSelected(DayOfWeek dayOfWeek) {
                Assert.assertTrue(dayOfWeek != null);

                DayOfWeekTimeEntry dayOfWeekTimeEntry = mDateTimeEntries.get(getAdapterPosition());
                Assert.assertTrue(dayOfWeekTimeEntry != null);

                dayOfWeekTimeEntry.setDayOfWeek(dayOfWeek);
            }

            public void onHourMinuteClick() {
                mHourMinutePickerPosition = getAdapterPosition();
                DayOfWeekTimeEntry dayOfWeekTimeEntry = mDateTimeEntries.get(mHourMinutePickerPosition);
                Assert.assertTrue(dayOfWeekTimeEntry != null);

                RadialTimePickerDialogFragment radialTimePickerDialogFragment = new RadialTimePickerDialogFragment();
                HourMinute startTime = dayOfWeekTimeEntry.getHourMinute();
                radialTimePickerDialogFragment.setStartTime(startTime.getHour(), startTime.getMinute());
                radialTimePickerDialogFragment.setOnTimeSetListener(mOnTimeSetListener);
                radialTimePickerDialogFragment.show(getChildFragmentManager(), TIME_PICKER_TAG);
            }

            public void delete() {
                int position = getAdapterPosition();
                mDateTimeEntries.remove(position);
                notifyItemRemoved(position);
            }
        }
    }

    public static class DayOfWeekTimeEntry implements Parcelable {
        private DayOfWeek mDayOfWeek;
        private TimePair mTimePair;
        private boolean mShowDelete = false;

        public DayOfWeekTimeEntry(DayOfWeek dayOfWeek, TimePair timePair, boolean showDelete) {
            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(timePair != null);

            mDayOfWeek = dayOfWeek;
            mTimePair = timePair;
            mShowDelete = showDelete;
        }

        public DayOfWeek getDayOfWeek() {
            return mDayOfWeek;
        }

        public Integer getCustomTimeId() {
            return mTimePair.CustomTimeId;
        }

        public HourMinute getHourMinute() {
            return mTimePair.HourMinute;
        }

        public void setDayOfWeek(DayOfWeek dayOfWeek) {
            Assert.assertTrue(dayOfWeek != null);
            mDayOfWeek = dayOfWeek;
        }

        public void setTimePair(TimePair timePair) {
            Assert.assertTrue(timePair != null);
            mTimePair = timePair;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeSerializable(mDayOfWeek);
            out.writeParcelable(mTimePair, 0);
            out.writeInt(mShowDelete ? 1 : 0);
        }

        public static final Parcelable.Creator<DayOfWeekTimeEntry> CREATOR = new Creator<DayOfWeekTimeEntry>() {
            public DayOfWeekTimeEntry createFromParcel(Parcel in) {
                DayOfWeek dayOfWeek = (DayOfWeek) in.readSerializable();
                TimePair timePair = in.readParcelable(TimePair.class.getClassLoader());
                int showDeleteInt = in.readInt();
                Assert.assertTrue(showDeleteInt == 0 || showDeleteInt == 1);
                boolean showDelete = (showDeleteInt == 1);

                return new DayOfWeekTimeEntry(dayOfWeek, timePair, showDelete);
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
}
