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
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity;
import com.krystianwsul.checkme.loaders.SingleScheduleLoader;
import com.krystianwsul.checkme.loaders.WeeklyScheduleLoader;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class WeeklyScheduleFragment extends Fragment implements ScheduleFragment, LoaderManager.LoaderCallbacks<SingleScheduleLoader.Data> {
    private static final String SCHEDULE_HINT_KEY = "scheduleHint";
    private static final String ROOT_TASK_ID_KEY = "rootTaskId";

    private static final String DATE_TIME_ENTRY_KEY = "dateTimeEntries";
    private static final String HOUR_MINUTE_PICKER_POSITION_KEY = "hourMinutePickerPosition";

    private static final String WEEKLY_SCHEDULE_DIALOG = "weeklyScheduleDialog";

    private int mHourMinutePickerPosition = -1;

    private RecyclerView mDailyScheduleTimes;
    private DayOfWeekTimeEntryAdapter mDayOfWeekTimeEntryAdapter;

    private Bundle mSavedInstanceState;

    private DayOfWeek mDayOfWeek = DayOfWeek.today();
    private TimePair mTimePair = null;

    private Integer mRootTaskId;
    private SingleScheduleLoader.Data mData;

    private FloatingActionButton mWeeklyScheduleFab;

    private List<DayOfWeekTimeEntry> mDayOfWeekTimeEntries;

    private boolean mFirst = true;

    private final WeeklyScheduleDialogFragment.WeeklyScheduleDialogListener mWeeklyScheduleDialogListener = new WeeklyScheduleDialogFragment.WeeklyScheduleDialogListener() {
        @Override
        public void onWeeklyScheduleDialogResult(DayOfWeek dayOfWeek, TimePairPersist timePairPersist) {
            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(timePairPersist != null);

            Assert.assertTrue(mHourMinutePickerPosition != -1);
            Assert.assertTrue(mData != null);

            DayOfWeekTimeEntry dayOfWeekTimeEntry = mDayOfWeekTimeEntries.get(mHourMinutePickerPosition);
            Assert.assertTrue(dayOfWeekTimeEntry != null);

            dayOfWeekTimeEntry.mDayOfWeek = dayOfWeek;
            dayOfWeekTimeEntry.mTimePairPersist = timePairPersist;

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
    public Loader<SingleScheduleLoader.Data> onCreateLoader(int id, Bundle args) {
        return new WeeklyScheduleLoader(getActivity(), mRootTaskId);
    }

    @Override
    public void onLoadFinished(Loader<SingleScheduleLoader.Data> loader, SingleScheduleLoader.Data data) {
        mData = data;

        if (mFirst && (mSavedInstanceState == null || !mSavedInstanceState.containsKey(DATE_TIME_ENTRY_KEY)) && mData.ScheduleDatas != null) {
            Assert.assertTrue(!mData.ScheduleDatas.isEmpty());
            Assert.assertTrue(mDayOfWeekTimeEntries == null);
            Assert.assertTrue(Stream.of(mData.ScheduleDatas).allMatch(scheduleData -> scheduleData.getScheduleType() == ScheduleType.WEEKLY)); // todo schedule hack

            mFirst = false;

            boolean showDelete = (mData.ScheduleDatas.size() > 1);
            mDayOfWeekTimeEntries = Stream.of(mData.ScheduleDatas)
                    .map(scheduleData -> new DayOfWeekTimeEntry(((SingleScheduleLoader.WeeklyScheduleData) scheduleData).DayOfWeek, ((SingleScheduleLoader.WeeklyScheduleData) scheduleData).TimePair, showDelete))
                    .collect(Collectors.toList());
        }

        mDayOfWeekTimeEntryAdapter = new DayOfWeekTimeEntryAdapter(getContext());
        mDailyScheduleTimes.setAdapter(mDayOfWeekTimeEntryAdapter);

        WeeklyScheduleDialogFragment weeklyScheduleDialogFragment = (WeeklyScheduleDialogFragment) getChildFragmentManager().findFragmentByTag(WEEKLY_SCHEDULE_DIALOG);
        if (weeklyScheduleDialogFragment != null)
            weeklyScheduleDialogFragment.initialize(data.CustomTimeDatas, mWeeklyScheduleDialogListener);

        mWeeklyScheduleFab.setOnClickListener(v -> {
            Assert.assertTrue(mDayOfWeekTimeEntryAdapter != null);
            mDayOfWeekTimeEntryAdapter.addDayOfWeekTimeEntry();
        });
        mWeeklyScheduleFab.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<SingleScheduleLoader.Data> loader) {
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

    private List<Pair<DayOfWeek, TimePair>> getDayOfWeekTimePairs() {
        Assert.assertTrue(!mDayOfWeekTimeEntries.isEmpty());

        return Stream.of(mDayOfWeekTimeEntries)
                .map(dayOfWeekTimeEntry -> new Pair<>(dayOfWeekTimeEntry.mDayOfWeek, dayOfWeekTimeEntry.mTimePairPersist.getTimePair()))
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

    private class DayOfWeekTimeEntryAdapter extends RecyclerView.Adapter<DayOfWeekTimeEntryAdapter.DayOfWeekTimeHolder> {
        private final Context mContext;

        public DayOfWeekTimeEntryAdapter(Context context) {
            Assert.assertTrue(context != null);

            mContext = context;
        }

        @Override
        public DayOfWeekTimeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View weeklyScheduleRow = LayoutInflater.from(mContext).inflate(R.layout.row_weekly_schedule, parent, false);

            TextView weeklyScheduleText = (TextView) weeklyScheduleRow.findViewById(R.id.weekly_schedule_text);
            Assert.assertTrue(weeklyScheduleText != null);

            ImageView weeklyScheduleImage = (ImageView) weeklyScheduleRow.findViewById(R.id.weekly_schedule_image);
            Assert.assertTrue(weeklyScheduleImage != null);

            return new DayOfWeekTimeHolder(weeklyScheduleRow, weeklyScheduleText, weeklyScheduleImage);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(final DayOfWeekTimeHolder dayOfWeekTimeHolder, int position) {
            final DayOfWeekTimeEntry dayOfWeekTimeEntry = mDayOfWeekTimeEntries.get(position);
            Assert.assertTrue(dayOfWeekTimeEntry != null);

            final ArrayAdapter<DayOfWeek> dayOfWeekAdapter = new ArrayAdapter<>(getContext(), R.layout.spinner_no_padding, DayOfWeek.values());
            dayOfWeekAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            if (dayOfWeekTimeEntry.mTimePairPersist.getCustomTimeId() != null) {
                SingleScheduleLoader.CustomTimeData customTimeData = mData.CustomTimeDatas.get(dayOfWeekTimeEntry.mTimePairPersist.getCustomTimeId());
                Assert.assertTrue(customTimeData != null);

                dayOfWeekTimeHolder.mWeeklyScheduleText.setText(dayOfWeekTimeEntry.mDayOfWeek + ", " + customTimeData.Name + " (" + customTimeData.HourMinutes.get(dayOfWeekTimeEntry.mDayOfWeek) + ")");
            } else {
                dayOfWeekTimeHolder.mWeeklyScheduleText.setText(dayOfWeekTimeEntry.mDayOfWeek + ", " + dayOfWeekTimeEntry.mTimePairPersist.getHourMinute().toString());
            }

            dayOfWeekTimeHolder.mWeeklyScheduleText.setOnClickListener(v -> dayOfWeekTimeHolder.onClick());

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
            public final TextView mWeeklyScheduleText;
            public final ImageView mWeeklyScheduleImage;

            public DayOfWeekTimeHolder(View weeklyScheduleRow, TextView weeklyScheduleText, ImageView weeklyScheduleImage) {
                super(weeklyScheduleRow);

                Assert.assertTrue(weeklyScheduleText != null);
                Assert.assertTrue(weeklyScheduleImage != null);

                mWeeklyScheduleText = weeklyScheduleText;
                mWeeklyScheduleImage = weeklyScheduleImage;
            }

            public void onClick() {
                Assert.assertTrue(mData != null);

                mHourMinutePickerPosition = getAdapterPosition();

                DayOfWeekTimeEntry dayOfWeekTimeEntry = mDayOfWeekTimeEntries.get(mHourMinutePickerPosition);
                Assert.assertTrue(dayOfWeekTimeEntry != null);

                WeeklyScheduleDialogFragment weeklyScheduleDialogFragment = WeeklyScheduleDialogFragment.newInstance(dayOfWeekTimeEntry.mDayOfWeek, dayOfWeekTimeEntry.mTimePairPersist);
                Assert.assertTrue(weeklyScheduleDialogFragment != null);

                weeklyScheduleDialogFragment.initialize(mData.CustomTimeDatas, mWeeklyScheduleDialogListener);

                weeklyScheduleDialogFragment.show(getChildFragmentManager(), WEEKLY_SCHEDULE_DIALOG);
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
        public TimePairPersist mTimePairPersist;
        private boolean mShowDelete = false;

        public DayOfWeekTimeEntry(DayOfWeek dayOfWeek, boolean showDelete) {
            Assert.assertTrue(dayOfWeek != null);

            mDayOfWeek = dayOfWeek;
            mTimePairPersist = new TimePairPersist();
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
        Assert.assertTrue(Stream.of(mData.ScheduleDatas)
                .allMatch(scheduleData -> scheduleData.getScheduleType() == ScheduleType.WEEKLY)); // todo schedule hack

        List<Pair<DayOfWeek, TimePair>> oldDayOfWeekTimePairs = Stream.of(mData.ScheduleDatas)
                .map(scheduleData -> new Pair<>(((SingleScheduleLoader.WeeklyScheduleData) scheduleData).DayOfWeek, ((SingleScheduleLoader.WeeklyScheduleData) scheduleData).TimePair))
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
