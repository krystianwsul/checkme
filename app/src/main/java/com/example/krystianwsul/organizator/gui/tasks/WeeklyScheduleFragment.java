package com.example.krystianwsul.organizator.gui.tasks;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.CustomTime;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.domainmodel.DomainLoader;
import com.example.krystianwsul.organizator.domainmodel.Task;
import com.example.krystianwsul.organizator.domainmodel.WeeklySchedule;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.NormalTime;
import com.example.krystianwsul.organizator.utils.time.Time;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class WeeklyScheduleFragment extends Fragment implements HourMinutePickerFragment.HourMinutePickerFragmentListener, ScheduleFragment, LoaderManager.LoaderCallbacks<DomainFactory> {
    private static final String DATE_TIME_ENTRY_KEY = "dateTimeEntries";
    private static final String HOUR_MINUTE_PICKER_POSITION_KEY = "hourMinutePickerPosition";
    private static final String ROOT_TASK_ID_KEY = "rootTaskId";

    private DomainFactory mDomainFactory;

    private int mHourMinutePickerPosition = -1;

    private RecyclerView mDailyScheduleTimes;
    private DayOfWeekTimeEntryAdapter mDayOfWeekTimeEntryAdapter;
    private Bundle mSavedInstanceState;

    public static WeeklyScheduleFragment newInstance() {
        return new WeeklyScheduleFragment();
    }

    public static WeeklyScheduleFragment newInstance(Task rootTask) {
        Assert.assertTrue(rootTask != null);
        Assert.assertTrue(rootTask.getCurrentSchedule(TimeStamp.getNow()) != null);
        Assert.assertTrue(rootTask.getCurrentSchedule(TimeStamp.getNow()) instanceof WeeklySchedule);

        WeeklyScheduleFragment weeklyScheduleFragment = new WeeklyScheduleFragment();

        Bundle args = new Bundle();
        args.putInt(ROOT_TASK_ID_KEY, rootTask.getId());

        weeklyScheduleFragment.setArguments(args);
        return weeklyScheduleFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Assert.assertTrue(context instanceof HourMinutePickerFragment.HourMinutePickerFragmentListener);
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

        FloatingActionButton weeklyScheduleFab = (FloatingActionButton) view.findViewById(R.id.weekly_schedule_fab);
        Assert.assertTrue(weeklyScheduleFab != null);

        weeklyScheduleFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Assert.assertTrue(mDayOfWeekTimeEntryAdapter != null);
                mDayOfWeekTimeEntryAdapter.addDayOfWeekTimeEntry();
            }
        });

        getLoaderManager().initLoader(0, null, this);
    }

    public void onHourMinutePickerFragmentResult(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);
        Assert.assertTrue(mHourMinutePickerPosition != -1);

        DayOfWeekTimeEntry dayOfWeekTimeEntry = mDayOfWeekTimeEntryAdapter.getDateTimeEntry(mHourMinutePickerPosition);

        dayOfWeekTimeEntry.setHourMinute(hourMinute);
        mDayOfWeekTimeEntryAdapter.notifyItemChanged(mHourMinutePickerPosition);

        mHourMinutePickerPosition = -1;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(DATE_TIME_ENTRY_KEY, mDayOfWeekTimeEntryAdapter.getDayOfWeekTimeEntries());
        outState.putInt(HOUR_MINUTE_PICKER_POSITION_KEY, mHourMinutePickerPosition);
    }

    @Override
    public boolean isValidTime() {
        return true;
    }

    private ArrayList<Pair<DayOfWeek, Time>> getDayOfWeekTimePairs() {
        Assert.assertTrue(!mDayOfWeekTimeEntryAdapter.getDayOfWeekTimeEntries().isEmpty());

        ArrayList<Pair<DayOfWeek, Time>> dayOfWeekTimePairs = new ArrayList<>();
        for (DayOfWeekTimeEntry dayOfWeekTimeEntry : mDayOfWeekTimeEntryAdapter.getDayOfWeekTimeEntries())
            dayOfWeekTimePairs.add(new Pair<>(dayOfWeekTimeEntry.getDayOfWeek(), dayOfWeekTimeEntry.getTime(mDomainFactory)));
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        return dayOfWeekTimePairs;
    }

    @Override
    public void createRootTask(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        ArrayList<Pair<DayOfWeek, Time>> dayOfWeekTimePairs = getDayOfWeekTimePairs();
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        mDomainFactory.createWeeklyScheduleRootTask(name, dayOfWeekTimePairs);

        mDomainFactory.save();

        TickService.startService(getActivity());
    }

    @Override
    public void updateRootTask(int rootTaskId, String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Task rootTask = mDomainFactory.getTask(rootTaskId);
        Assert.assertTrue(rootTask != null);

        ArrayList<Pair<DayOfWeek, Time>> dayOfWeekTimePairs = getDayOfWeekTimePairs();
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        mDomainFactory.updateWeeklyScheduleRootTask(rootTask, name, dayOfWeekTimePairs);

        mDomainFactory.save();

        TickService.startService(getActivity());
    }

    @Override
    public void createRootJoinTask(String name, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        ArrayList<Pair<DayOfWeek, Time>> dayOfWeekTimePairs = getDayOfWeekTimePairs();
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        ArrayList<Task> joinTasks = new ArrayList<>();
        for (Integer joinTaskId : joinTaskIds) {
            Task joinTask = mDomainFactory.getTask(joinTaskId);
            Assert.assertTrue(joinTask != null);

            joinTasks.add(joinTask);
        }

        mDomainFactory.createWeeklyScheduleJoinRootTask(name, dayOfWeekTimePairs, joinTasks);

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

        if (mSavedInstanceState != null) {
            List<DayOfWeekTimeEntry> dateTimeEntries = mSavedInstanceState.getParcelableArrayList(DATE_TIME_ENTRY_KEY);
            mDayOfWeekTimeEntryAdapter = new DayOfWeekTimeEntryAdapter(getContext(), dateTimeEntries);

            mHourMinutePickerPosition = mSavedInstanceState.getInt(HOUR_MINUTE_PICKER_POSITION_KEY, -2);
            Assert.assertTrue(mHourMinutePickerPosition != -2);
        } else if (args != null) {
            Assert.assertTrue(args.containsKey(ROOT_TASK_ID_KEY));
            int rootTaskId = args.getInt(ROOT_TASK_ID_KEY, -1);
            Assert.assertTrue(rootTaskId != -1);

            Task rootTask = domainFactory.getTask(rootTaskId);
            Assert.assertTrue(rootTask != null);

            WeeklySchedule weeklySchedule = (WeeklySchedule) rootTask.getCurrentSchedule(TimeStamp.getNow());
            Assert.assertTrue(weeklySchedule != null);
            Assert.assertTrue(weeklySchedule.current(TimeStamp.getNow()));

            ArrayList<DayOfWeekTimeEntry> dayOfWeekTimeEntries = new ArrayList<>();
            boolean showDelete = (weeklySchedule.getDayOfWeekTimes().size() > 1);
            for (Pair<DayOfWeek, Time> dayOfWeekTime : weeklySchedule.getDayOfWeekTimes())
                dayOfWeekTimeEntries.add(new DayOfWeekTimeEntry(dayOfWeekTime.first, dayOfWeekTime.second, showDelete));
            mDayOfWeekTimeEntryAdapter = new DayOfWeekTimeEntryAdapter(getContext(), dayOfWeekTimeEntries);

            mHourMinutePickerPosition = -1;
        } else {
            mDayOfWeekTimeEntryAdapter = new DayOfWeekTimeEntryAdapter(getContext());
            mHourMinutePickerPosition = -1;
        }
        mDailyScheduleTimes.setAdapter(mDayOfWeekTimeEntryAdapter);
    }

    @Override
    public void onLoaderReset(Loader<DomainFactory> loader) {
        mDomainFactory = null;
        mDayOfWeekTimeEntryAdapter = null;
        mDailyScheduleTimes.setAdapter(null);
    }

    private class DayOfWeekTimeEntryAdapter extends RecyclerView.Adapter<DayOfWeekTimeEntryAdapter.DayOfWeekTimeHolder> {
        private final ArrayList<DayOfWeekTimeEntry> mDateTimeEntries;
        private final Context mContext;

        public DayOfWeekTimeEntryAdapter(Context context) {
            Assert.assertTrue(context != null);

            mContext = context;
            mDateTimeEntries = new ArrayList<>();
            mDateTimeEntries.add(new DayOfWeekTimeEntry(DayOfWeek.today(), HourMinute.getNow(), false));
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
            RelativeLayout weeklyScheduleRow = (RelativeLayout) LayoutInflater.from(mContext).inflate(R.layout.weekly_schedule_row, parent, false);

            Spinner weeklyScheduleDay = (Spinner) weeklyScheduleRow.findViewById(R.id.weekly_schedule_day);

            TimePickerView weeklyScheduleTime = (TimePickerView) weeklyScheduleRow.findViewById(R.id.weekly_schedule_time);
            Assert.assertTrue(weeklyScheduleTime != null);

            weeklyScheduleTime.setDomainFactory(mDomainFactory);

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

            CustomTime customTime = dayOfWeekTimeEntry.getCustomTime(mDomainFactory);
            if (customTime != null) {
                Assert.assertTrue(dayOfWeekTimeEntry.getHourMinute() == null);
                dayOfWeekTimeHolder.mWeeklyScheduleTime.setCustomTime(customTime);
            } else {
                Assert.assertTrue(dayOfWeekTimeEntry.getHourMinute() != null);
                dayOfWeekTimeHolder.mWeeklyScheduleTime.setHourMinute(dayOfWeekTimeEntry.getHourMinute());
            }

            dayOfWeekTimeHolder.mWeeklyScheduleTime.setOnTimeSelectedListener(new TimePickerView.OnTimeSelectedListener() {
                @Override
                public void onCustomTimeSelected(CustomTime customTime) {
                    Assert.assertTrue(customTime != null);
                    dayOfWeekTimeEntry.setCustomTime(customTime);
                }

                @Override
                public void onHourMinuteSelected(HourMinute hourMinute) {
                    Assert.assertTrue(hourMinute != null);
                    dayOfWeekTimeEntry.setHourMinute(hourMinute);
                }

                @Override
                public void onHourMinuteClick() {
                    dayOfWeekTimeHolder.onHourMinuteClick();
                }
            });

            dayOfWeekTimeHolder.mWeeklyScheduleImage.setVisibility(dayOfWeekTimeEntry.getShowDelete() ? View.VISIBLE : View.INVISIBLE);

            dayOfWeekTimeHolder.mWeeklyScheduleImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Assert.assertTrue(mDateTimeEntries.size() > 1);
                    dayOfWeekTimeHolder.delete();

                    if (mDateTimeEntries.size() == 1) {
                        mDateTimeEntries.get(0).setShowDelete(false);
                        notifyItemChanged(0);
                    }
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

            DayOfWeekTimeEntry dayOfWeekTimeEntry = new DayOfWeekTimeEntry(DayOfWeek.today(), HourMinute.getNow(), true);
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

            public DayOfWeekTimeHolder(RelativeLayout weeklyScheduleRow, Spinner weeklyScheduleDay, TimePickerView weeklyScheduleTime, ImageView weeklyScheduleImage) {
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

                FragmentManager fragmentManager = getChildFragmentManager();
                HourMinutePickerFragment hourMinutePickerFragment = HourMinutePickerFragment.newInstance(getActivity(), dayOfWeekTimeEntry.getHourMinute());
                hourMinutePickerFragment.show(fragmentManager, "time");
            }

            public void delete() {
                int position = getAdapterPosition();
                mDateTimeEntries.remove(position);
                notifyItemRemoved(position);
            }
        }
    }

    public static class DayOfWeekTimeEntry implements Parcelable {
        private int EMPTY_CUSTOM_TIME = -1;

        private DayOfWeek mDayOfWeek;
        private int mCustomTimeId = EMPTY_CUSTOM_TIME;
        private HourMinute mHourMinute;
        private boolean mShowDelete = false;

        public DayOfWeekTimeEntry(DayOfWeek dayOfWeek, CustomTime customTime, boolean showDelete) {
            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(customTime != null);

            mDayOfWeek = dayOfWeek;
            setCustomTime(customTime);
            mShowDelete = showDelete;
        }

        public DayOfWeekTimeEntry(DayOfWeek dayOfWeek, int customTimeId, boolean showDelete) {
            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(customTimeId != EMPTY_CUSTOM_TIME);

            mDayOfWeek = dayOfWeek;
            setCustomTime(customTimeId);
            mShowDelete = showDelete;
        }

        public DayOfWeekTimeEntry(DayOfWeek dayOfWeek, HourMinute hourMinute, boolean showDelete) {
            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(hourMinute != null);

            mDayOfWeek = dayOfWeek;
            setHourMinute(hourMinute);
            mShowDelete = showDelete;
        }

        public DayOfWeekTimeEntry(DayOfWeek dayOfWeek, Time time, boolean showDelete) {
            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(time != null);

            mDayOfWeek = dayOfWeek;

            if (time instanceof CustomTime) {
                setCustomTime((CustomTime) time);
            } else {
                Assert.assertTrue(time instanceof NormalTime);
                setHourMinute(((NormalTime) time).getHourMinute());
            }

            mShowDelete = showDelete;

        }

        public DayOfWeek getDayOfWeek() {
            return mDayOfWeek;
        }

        public CustomTime getCustomTime(DomainFactory domainFactory) {
            Assert.assertTrue(domainFactory != null);

            if (mCustomTimeId == EMPTY_CUSTOM_TIME)
                return null;
            else
                return domainFactory.getCustomTime(mCustomTimeId);
        }

        public HourMinute getHourMinute() {
            return mHourMinute;
        }

        public void setDayOfWeek(DayOfWeek dayOfWeek) {
            Assert.assertTrue(dayOfWeek != null);
            mDayOfWeek = dayOfWeek;
        }

        public void setCustomTime(CustomTime customTime) {
            Assert.assertTrue(customTime != null);

            mCustomTimeId = customTime.getId();
            mHourMinute = null;
        }

        public void setCustomTime(int customTimeId) {
            Assert.assertTrue(customTimeId != EMPTY_CUSTOM_TIME);

            mCustomTimeId = customTimeId;
            mHourMinute = null;
        }

        public void setHourMinute(HourMinute hourMinute) {
            Assert.assertTrue(hourMinute != null);

            mHourMinute = hourMinute;
            mCustomTimeId = EMPTY_CUSTOM_TIME;
        }

        public Time getTime(DomainFactory domainFactory) {
            Assert.assertTrue(domainFactory != null);

            if (mCustomTimeId != EMPTY_CUSTOM_TIME) {
                Assert.assertTrue(mHourMinute == null);
                return domainFactory.getCustomTime(mCustomTimeId);
            } else {
                Assert.assertTrue(mHourMinute != null);
                return new NormalTime(mHourMinute);
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            Assert.assertTrue((mCustomTimeId == EMPTY_CUSTOM_TIME) != (mHourMinute == null));

            out.writeSerializable(mDayOfWeek);

            out.writeInt(mCustomTimeId);

            if (mHourMinute != null) {
                out.writeInt(mHourMinute.getHour());
                out.writeInt(mHourMinute.getMinute());
            } else {
                out.writeInt(-1);
                out.writeInt(-1);
            }

            out.writeInt(mShowDelete ? 1 : 0);
        }

        public static final Parcelable.Creator<DayOfWeekTimeEntry> CREATOR = new Creator<DayOfWeekTimeEntry>() {
            public DayOfWeekTimeEntry createFromParcel(Parcel in) {
                DayOfWeek dayOfWeek = (DayOfWeek) in.readSerializable();
                int customTimeId = in.readInt();
                int hour = in.readInt();
                int minute = in.readInt();
                int showDeleteInt = in.readInt();
                Assert.assertTrue(showDeleteInt == 0 || showDeleteInt == 1);
                boolean showDelete = (showDeleteInt == 1);

                Assert.assertTrue((hour == -1) == (minute == -1));
                Assert.assertTrue((hour == -1) != (customTimeId == -1));

                if (customTimeId != -1)
                    return new DayOfWeekTimeEntry(dayOfWeek, customTimeId, showDelete);
                else
                    return new DayOfWeekTimeEntry(dayOfWeek, new HourMinute(hour, minute), showDelete);
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
