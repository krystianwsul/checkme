package com.krystianwsul.checkme.gui.tasks;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity;
import com.krystianwsul.checkme.loaders.ScheduleLoader;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WeeklyScheduleFragment extends RepeatingScheduleFragment implements ScheduleFragment, LoaderManager.LoaderCallbacks<ScheduleLoader.Data> {
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
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSavedInstanceState = savedInstanceState;

        View view = getView();
        Assert.assertTrue(view != null);

        mScheduleTimes = (RecyclerView) view.findViewById(R.id.schedule_recycler);
        mScheduleTimes.setLayoutManager(new LinearLayoutManager(getContext()));

        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(ROOT_TASK_ID_KEY)) {
                Assert.assertTrue(!args.containsKey(SCHEDULE_HINT_KEY));

                mRootTaskId = args.getInt(ROOT_TASK_ID_KEY, -1);
                Assert.assertTrue(mRootTaskId != -1);
            } else {
                Assert.assertTrue(args.containsKey(SCHEDULE_HINT_KEY));

                mScheduleHint = args.getParcelable(SCHEDULE_HINT_KEY);
                Assert.assertTrue(mScheduleHint != null);
            }
        }

        if (mSavedInstanceState != null && mSavedInstanceState.containsKey(SCHEDULE_ENTRY_KEY)) {
            mScheduleEntries = mSavedInstanceState.getParcelableArrayList(SCHEDULE_ENTRY_KEY);

            mHourMinutePickerPosition = mSavedInstanceState.getInt(HOUR_MINUTE_PICKER_POSITION_KEY, -2);
            Assert.assertTrue(mHourMinutePickerPosition != -2);
        } else if (args != null && args.containsKey(ROOT_TASK_ID_KEY)) {
            mHourMinutePickerPosition = -1;
        } else {
            mScheduleEntries = new ArrayList<>();
            if (mScheduleHint != null)
                mScheduleEntries.add(new WeeklyScheduleEntry(mScheduleHint.mDate.getDayOfWeek(), mScheduleHint.mTimePair, false));
            else
                mScheduleEntries.add(new WeeklyScheduleEntry(DayOfWeek.today(), false));

            mHourMinutePickerPosition = -1;
        }

        mScheduleFab = (FloatingActionButton) view.findViewById(R.id.schedule_fab);
        Assert.assertTrue(mScheduleFab != null);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("WeeklyScheduleFragment.onResume");

        super.onResume();
    }

    @Override
    public Loader<ScheduleLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ScheduleLoader(getActivity(), mRootTaskId);
    }

    @Override
    public void onLoadFinished(Loader<ScheduleLoader.Data> loader, ScheduleLoader.Data data) {
        mData = data;

        if (mFirst && (mSavedInstanceState == null || !mSavedInstanceState.containsKey(SCHEDULE_ENTRY_KEY)) && mData.ScheduleDatas != null) {
            Assert.assertTrue(!mData.ScheduleDatas.isEmpty());
            Assert.assertTrue(mScheduleEntries == null);
            Assert.assertTrue(Stream.of(mData.ScheduleDatas).allMatch(scheduleData -> scheduleData.getScheduleType() == ScheduleType.WEEKLY)); // todo schedule hack

            mFirst = false;

            boolean showDelete = (mData.ScheduleDatas.size() > 1);
            mScheduleEntries = Stream.of(mData.ScheduleDatas)
                    .map(scheduleData -> new WeeklyScheduleEntry(((ScheduleLoader.WeeklyScheduleData) scheduleData).DayOfWeek, ((ScheduleLoader.WeeklyScheduleData) scheduleData).TimePair, showDelete))
                    .collect(Collectors.toList());
        }

        mScheduleAdapter = new ScheduleAdapter(getContext());
        mScheduleTimes.setAdapter(mScheduleAdapter);

        WeeklyScheduleDialogFragment weeklyScheduleDialogFragment = (WeeklyScheduleDialogFragment) getChildFragmentManager().findFragmentByTag(WEEKLY_SCHEDULE_DIALOG);
        if (weeklyScheduleDialogFragment != null)
            weeklyScheduleDialogFragment.initialize(data.CustomTimeDatas, mWeeklyScheduleDialogListener);

        mScheduleFab.setOnClickListener(v -> {
            Assert.assertTrue(mScheduleAdapter != null);

            WeeklyScheduleEntry weeklyScheduleEntry;
            if (mScheduleHint != null)
                weeklyScheduleEntry = new WeeklyScheduleEntry(mScheduleHint.mDate.getDayOfWeek(), mScheduleHint.mTimePair, true);
            else
                weeklyScheduleEntry = new WeeklyScheduleEntry(DayOfWeek.today(), true);

            mScheduleAdapter.addScheduleEntry(weeklyScheduleEntry);
        });
        mScheduleFab.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<ScheduleLoader.Data> loader) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mData != null) {
            Assert.assertTrue(mScheduleAdapter != null);

            outState.putParcelableArrayList(SCHEDULE_ENTRY_KEY, new ArrayList<>(mScheduleEntries));
            outState.putInt(HOUR_MINUTE_PICKER_POSITION_KEY, mHourMinutePickerPosition);
        }
    }

    private List<Pair<DayOfWeek, TimePair>> getDayOfWeekTimePairs() {
        Assert.assertTrue(!mScheduleEntries.isEmpty());

        return Stream.of(mScheduleEntries)
                .map(dayOfWeekTimeEntry -> new Pair<>(((WeeklyScheduleEntry) dayOfWeekTimeEntry).mDayOfWeek, ((WeeklyScheduleEntry) dayOfWeekTimeEntry).mTimePairPersist.getTimePair()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean createRootTask(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        if (mData == null)
            return false;

        List<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs = getDayOfWeekTimePairs();
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

        List<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs = getDayOfWeekTimePairs();
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

        List<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs = getDayOfWeekTimePairs();
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).createWeeklyScheduleJoinRootTask(mData.DataId, name, dayOfWeekTimePairs, joinTaskIds);

        TickService.startService(getActivity());

        return true;
    }

    public static class WeeklyScheduleEntry extends RepeatingScheduleFragment.ScheduleEntry {
        public DayOfWeek mDayOfWeek;
        public TimePairPersist mTimePairPersist;
        private boolean mShowDelete = false;

        public WeeklyScheduleEntry(DayOfWeek dayOfWeek, boolean showDelete) {
            Assert.assertTrue(dayOfWeek != null);

            mDayOfWeek = dayOfWeek;
            mTimePairPersist = new TimePairPersist();
            mShowDelete = showDelete;
        }

        private WeeklyScheduleEntry(DayOfWeek dayOfWeek, TimePair timePair, boolean showDelete) {
            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(timePair != null);

            mDayOfWeek = dayOfWeek;
            mTimePairPersist = new TimePairPersist(timePair);
            mShowDelete = showDelete;
        }

        public WeeklyScheduleEntry(Parcel parcel) {
            Assert.assertTrue(parcel != null);

            mDayOfWeek = (DayOfWeek) parcel.readSerializable();
            Assert.assertTrue(mDayOfWeek != null);

            mTimePairPersist = parcel.readParcelable(TimePairPersist.class.getClassLoader());
            Assert.assertTrue(mTimePairPersist != null);

            int showDeleteInt = parcel.readInt();
            Assert.assertTrue(showDeleteInt == 0 || showDeleteInt == 1);
            mShowDelete = (showDeleteInt == 1);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeSerializable(ScheduleType.WEEKLY);

            out.writeSerializable(mDayOfWeek);
            out.writeParcelable(mTimePairPersist, 0);
            out.writeInt(mShowDelete ? 1 : 0);
        }

        @Override
        public boolean getShowDelete() {
            return mShowDelete;
        }

        @Override
        public void setShowDelete(boolean delete) {
            mShowDelete = delete;
        }

        public static final Creator<ScheduleEntry> CREATOR = ScheduleEntry.CREATOR;

        @Override
        public ScheduleType getScheduleType() {
            return ScheduleType.WEEKLY;
        }

        @Override
        public String getText(Map<Integer, ScheduleLoader.CustomTimeData> customTimeDatas) {
            Assert.assertTrue(customTimeDatas != null);

            if (mTimePairPersist.getCustomTimeId() != null) {
                ScheduleLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePairPersist.getCustomTimeId());
                Assert.assertTrue(customTimeData != null);

                return mDayOfWeek + ", " + customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDayOfWeek) + ")";
            } else {
                return mDayOfWeek + ", " + mTimePairPersist.getHourMinute().toString();
            }
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean dataChanged() {
        Assert.assertTrue(mRootTaskId != null);

        if (mData == null)
            return false;

        Assert.assertTrue(mScheduleAdapter != null);

        Assert.assertTrue(mData.ScheduleDatas != null);
        Assert.assertTrue(Stream.of(mData.ScheduleDatas)
                .allMatch(scheduleData -> scheduleData.getScheduleType() == ScheduleType.WEEKLY)); // todo schedule hack

        List<Pair<DayOfWeek, TimePair>> oldDayOfWeekTimePairs = Stream.of(mData.ScheduleDatas)
                .map(scheduleData -> new Pair<>(((ScheduleLoader.WeeklyScheduleData) scheduleData).DayOfWeek, ((ScheduleLoader.WeeklyScheduleData) scheduleData).TimePair))
                .sortBy(Pair::hashCode)
                .collect(Collectors.toList());

        List<Pair<DayOfWeek, TimePair>> newDayOfWeekTimePairs = Stream.of(mScheduleEntries)
                .map(dayOfWeekTimeEntry -> new Pair<>(((WeeklyScheduleEntry) dayOfWeekTimeEntry).mDayOfWeek, ((WeeklyScheduleEntry) dayOfWeekTimeEntry).mTimePairPersist.getTimePair()))
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
            WeeklyScheduleEntry weeklyScheduleEntry = (WeeklyScheduleEntry) mScheduleEntries.get(mHourMinutePickerPosition);
            Assert.assertTrue(weeklyScheduleEntry != null);

            weeklyScheduleEntry.mTimePairPersist.setCustomTimeId(resultCode);
        }

        mHourMinutePickerPosition = -1;
    }
}
