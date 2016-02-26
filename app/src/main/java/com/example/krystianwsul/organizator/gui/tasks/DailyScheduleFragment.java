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
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.loaders.DailyScheduleLoader;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.time.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DailyScheduleFragment extends Fragment implements HourMinutePickerFragment.HourMinutePickerFragmentListener, ScheduleFragment, LoaderManager.LoaderCallbacks<DailyScheduleLoader.Data> {
    private static final String TIME_ENTRY_KEY = "timeEntries";
    private static final String HOUR_MINUTE_PICKER_POSITION_KEY = "hourMinutePickerPosition";
    private static final String ROOT_TASK_ID_KEY = "rootTaskId";

    private int mHourMinutePickerPosition = -1;

    private TimeEntryAdapter mTimeEntryAdapter;
    private RecyclerView mDailyScheduleTimes;

    private Bundle mSavedInstanceState;

    private Integer mRootTaskId;
    private DailyScheduleLoader.Data mData;

    public static DailyScheduleFragment newInstance() {
        return new DailyScheduleFragment();
    }

    public static DailyScheduleFragment newInstance(int rootTaskId) {
        DailyScheduleFragment dailyScheduleFragment = new DailyScheduleFragment();

        Bundle args = new Bundle();
        args.putInt(ROOT_TASK_ID_KEY, rootTaskId);

        dailyScheduleFragment.setArguments(args);
        return dailyScheduleFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Assert.assertTrue(context instanceof HourMinutePickerFragment.HourMinutePickerFragmentListener);
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
            Assert.assertTrue(args.containsKey(ROOT_TASK_ID_KEY));
            mRootTaskId = args.getInt(ROOT_TASK_ID_KEY, -1);
            Assert.assertTrue(mRootTaskId != -1);
        }

        FloatingActionButton dailyScheduleFab = (FloatingActionButton) view.findViewById(R.id.daily_schedule_fab);
        Assert.assertTrue(dailyScheduleFab != null);

        dailyScheduleFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Assert.assertTrue(mTimeEntryAdapter != null);
                mTimeEntryAdapter.addTimeEntry();
            }
        });

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
        } else if (args != null) {
            Assert.assertTrue(mData.ScheduleDatas != null);
            Assert.assertTrue(!mData.ScheduleDatas.isEmpty());

            ArrayList<TimeEntry> timeEntries = new ArrayList<>();
            boolean showDelete = (mData.ScheduleDatas.size() > 1);
            for (DailyScheduleLoader.ScheduleData scheduleData : mData.ScheduleDatas) {
                if (scheduleData.CustomTimeId != null)
                    timeEntries.add(new TimeEntry(scheduleData.CustomTimeId, showDelete));
                else
                    timeEntries.add(new TimeEntry(scheduleData.HourMinute, showDelete));
            }
            mTimeEntryAdapter = new TimeEntryAdapter(getContext(), timeEntries);

            mHourMinutePickerPosition = -1;
        } else {
            mTimeEntryAdapter = new TimeEntryAdapter(getContext());
            mHourMinutePickerPosition = -1;
        }
        mDailyScheduleTimes.setAdapter(mTimeEntryAdapter);
    }

    @Override
    public void onLoaderReset(Loader<DailyScheduleLoader.Data> loader) {
    }

    public void onHourMinutePickerFragmentResult(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);
        Assert.assertTrue(mHourMinutePickerPosition != -1);

        TimeEntry timeEntry = mTimeEntryAdapter.getTimeEntry(mHourMinutePickerPosition);
        Assert.assertTrue(timeEntry != null);

        timeEntry.setHourMinute(hourMinute);
        mTimeEntryAdapter.notifyItemChanged(mHourMinutePickerPosition);

        mHourMinutePickerPosition = -1;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(TIME_ENTRY_KEY, mTimeEntryAdapter.getTimeEntries());
        outState.putInt(HOUR_MINUTE_PICKER_POSITION_KEY, mHourMinutePickerPosition);
    }

    @Override
    public boolean isValidTime() {
        return true;
    }

    private ArrayList<Pair<Integer, HourMinute>> getTimePairs() {
        Assert.assertTrue(!mTimeEntryAdapter.getTimeEntries().isEmpty());

        ArrayList<Pair<Integer, HourMinute>> timePairs = new ArrayList<>();

        for (TimeEntry timeEntry : mTimeEntryAdapter.getTimeEntries())
            timePairs.add(new Pair<>(timeEntry.getCustomTimeId(), timeEntry.getHourMinute()));
        Assert.assertTrue(!timePairs.isEmpty());

        return timePairs;
    }

    @Override
    public void createRootTask(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Assert.assertTrue(mRootTaskId == null);

        ArrayList<Pair<Integer, HourMinute>> timePairs = getTimePairs();
        Assert.assertTrue(!timePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).createDailyScheduleRootTask(mData.DataId, name, timePairs);

        TickService.startService(getActivity());
    }

    @Override
    public void updateRootTask(int rootTaskId, String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Assert.assertTrue(mRootTaskId != null);

        ArrayList<Pair<Integer, HourMinute>> timePairs = getTimePairs();
        Assert.assertTrue(!timePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).updateDailyScheduleRootTask(mData.DataId, mRootTaskId, name, timePairs);

        TickService.startService(getActivity());
    }

    @Override
    public void createRootJoinTask(String name, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        Assert.assertTrue(mRootTaskId == null);

        ArrayList<Pair<Integer, HourMinute>> timePairs = getTimePairs();
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
            mTimeEntries.add(new TimeEntry(HourMinute.getNow(), false));
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
            RelativeLayout dailyScheduleRow = (RelativeLayout) LayoutInflater.from(mContext).inflate(R.layout.daily_schedule_row, parent, false);

            TimePickerView dailyScheduleTime = (TimePickerView) dailyScheduleRow.findViewById(R.id.daily_schedule_time);
            Assert.assertTrue(dailyScheduleTime != null);

            HashMap<Integer, TimePickerView.CustomTimeData> customTimeDatas = new HashMap<>();
            for (DailyScheduleLoader.CustomTimeData customTimeData : mData.CustomTimeDatas.values())
                customTimeDatas.put(customTimeData.Id, new TimePickerView.CustomTimeData(customTimeData.Id, customTimeData.Name, customTimeData.HourMinutes));
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
                    timeEntry.setCustomTimeId(customTimeId);
                }

                @Override
                public void onHourMinuteSelected(HourMinute hourMinute) {
                    timeEntry.setHourMinute(hourMinute);
                }

                @Override
                public void onHourMinuteClick() {
                    timeHolder.onHourMinuteClick();
                }
            });

            timeHolder.mDailyScheduleImage.setVisibility(timeEntry.getShowDelete() ? View.VISIBLE : View.INVISIBLE);

            timeHolder.mDailyScheduleImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Assert.assertTrue(mTimeEntries.size() > 1);
                    timeHolder.delete();

                    if (mTimeEntries.size() == 1) {
                        mTimeEntries.get(0).setShowDelete(false);
                        notifyItemChanged(0);
                    }
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

            TimeEntry timeEntry = new TimeEntry(HourMinute.getNow(), true);
            mTimeEntries.add(position, timeEntry);
            notifyItemInserted(position);
        }

        public ArrayList<TimeEntry> getTimeEntries() {
            return mTimeEntries;
        }

        public class TimeHolder extends RecyclerView.ViewHolder {
            public final TimePickerView mDailyScheduleTime;
            public final ImageView mDailyScheduleImage;

            public TimeHolder(RelativeLayout dailyScheduleRow, TimePickerView dailyScheduleTime, ImageView dailyScheduleImage) {
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

                FragmentManager fragmentManager = getChildFragmentManager();
                HourMinutePickerFragment hourMinutePickerFragment = HourMinutePickerFragment.newInstance(getActivity(), timeEntry.getHourMinute());
                hourMinutePickerFragment.show(fragmentManager, "time");
            }

            public void delete() {
                int position = getAdapterPosition();
                mTimeEntries.remove(position);
                notifyItemRemoved(position);
            }
        }
    }

    public static class TimeEntry implements Parcelable {
        private Integer mCustomTimeId;
        private HourMinute mHourMinute;
        private boolean mShowDelete;

        public TimeEntry(int customTimeId, boolean showDelete) {
            setCustomTimeId(customTimeId);
            mShowDelete = showDelete;
        }

        public TimeEntry(HourMinute hourMinute, boolean showDelete) {
            Assert.assertTrue(hourMinute != null);

            setHourMinute(hourMinute);
            mShowDelete = showDelete;
        }

        public Integer getCustomTimeId() {
            return mCustomTimeId;
        }

        public HourMinute getHourMinute() {
            return mHourMinute;
        }

        public void setCustomTimeId(int customTimeId) {
            mCustomTimeId = customTimeId;
            mHourMinute = null;
        }

        public void setHourMinute(HourMinute hourMinute) {
            Assert.assertTrue(hourMinute != null);

            mHourMinute = hourMinute;
            mCustomTimeId = null;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            Assert.assertTrue((mCustomTimeId == null) != (mHourMinute == null));

            if (mCustomTimeId != null)
                out.writeInt(mCustomTimeId);
            else
                out.writeInt(-1);

            if (mHourMinute != null) {
                out.writeInt(mHourMinute.getHour());
                out.writeInt(mHourMinute.getMinute());
            } else {
                out.writeInt(-1);
                out.writeInt(-1);
            }

            out.writeInt(mShowDelete ? 1 : 0);
        }

        public static final Parcelable.Creator<TimeEntry> CREATOR = new Parcelable.Creator<TimeEntry>() {
            public TimeEntry createFromParcel(Parcel in) {
                int customTimeId = in.readInt();
                int hour = in.readInt();
                int minute = in.readInt();
                int showDeleteInt = in.readInt();
                Assert.assertTrue(showDeleteInt == 0 || showDeleteInt == 1);
                boolean showDelete = (showDeleteInt == 1);

                Assert.assertTrue((hour == -1) == (minute == -1));
                Assert.assertTrue((hour == -1) != (customTimeId == -1));

                if (customTimeId != -1)
                    return new TimeEntry(customTimeId, showDelete);
                else
                    return new TimeEntry(new HourMinute(hour, minute), showDelete);
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