package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.content.Intent;
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
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity;
import com.krystianwsul.checkme.loaders.ScheduleLoader;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class RepeatingScheduleFragment extends Fragment implements ScheduleFragment, LoaderManager.LoaderCallbacks<ScheduleLoader.Data> {
    static final String SCHEDULE_HINT_KEY = "scheduleHint";
    static final String ROOT_TASK_ID_KEY = "rootTaskId";

    private static final String HOUR_MINUTE_PICKER_POSITION_KEY = "hourMinutePickerPosition";

    private static final String SCHEDULE_ENTRY_KEY = "scheduleEntries";

    private static final String SINGLE_SCHEDULE_DIALOG_TAG = "singleScheduleDialog";
    private static final String DAILY_SCHEDULE_DIALOG_TAG = "dailyScheduleDialog";
    private static final String WEEKLY_SCHEDULE_DIALOG_TAG = "weeklyScheduleDialog";

    private int mHourMinutePickerPosition = -1;

    private RecyclerView mScheduleTimes;
    private ScheduleAdapter mScheduleAdapter;

    CreateTaskActivity.ScheduleHint mScheduleHint;

    private Bundle mSavedInstanceState;

    private Integer mRootTaskId;
    private ScheduleLoader.Data mData;

    private FloatingActionButton mScheduleFab;

    private boolean mFirst = true;

    private List<ScheduleEntry> mScheduleEntries;

    private final SingleScheduleDialogFragment.SingleScheduleDialogListener mSingleScheduleDialogListener = new SingleScheduleDialogFragment.SingleScheduleDialogListener() {
        @Override
        public void onSingleScheduleDialogResult(Date date, TimePairPersist timePairPersist) {
            Assert.assertTrue(date != null);
            Assert.assertTrue(timePairPersist != null);

            Assert.assertTrue(mHourMinutePickerPosition != -1);
            Assert.assertTrue(mData != null);

            SingleScheduleEntry singleScheduleEntry = (SingleScheduleEntry) mScheduleEntries.get(mHourMinutePickerPosition);
            Assert.assertTrue(singleScheduleEntry != null);

            singleScheduleEntry.mDate = date;
            singleScheduleEntry.mTimePairPersist = timePairPersist;

            mScheduleAdapter.notifyItemChanged(mHourMinutePickerPosition);

            mHourMinutePickerPosition = -1;
        }
    };

    private final DailyScheduleDialogFragment.DailyScheduleDialogListener mDailyScheduleDialogListener = new DailyScheduleDialogFragment.DailyScheduleDialogListener() {
        @Override
        public void onDailyScheduleDialogResult(TimePairPersist timePairPersist) {
            Assert.assertTrue(timePairPersist != null);
            Assert.assertTrue(mHourMinutePickerPosition != -1);
            Assert.assertTrue(mData != null);

            DailyScheduleEntry dailyScheduleEntry = (DailyScheduleEntry) mScheduleEntries.get(mHourMinutePickerPosition);
            Assert.assertTrue(dailyScheduleEntry != null);

            dailyScheduleEntry.mTimePairPersist = timePairPersist;
            mScheduleAdapter.notifyItemChanged(mHourMinutePickerPosition);

            mHourMinutePickerPosition = -1;
        }
    };

    private final WeeklyScheduleDialogFragment.WeeklyScheduleDialogListener mWeeklyScheduleDialogListener = new WeeklyScheduleDialogFragment.WeeklyScheduleDialogListener() {
        @Override
        public void onWeeklyScheduleDialogResult(DayOfWeek dayOfWeek, TimePairPersist timePairPersist) {
            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(timePairPersist != null);

            Assert.assertTrue(mHourMinutePickerPosition != -1);
            Assert.assertTrue(mData != null);

            WeeklyScheduleEntry weeklyScheduleEntry = (WeeklyScheduleEntry) mScheduleEntries.get(mHourMinutePickerPosition);
            Assert.assertTrue(weeklyScheduleEntry != null);

            weeklyScheduleEntry.mDayOfWeek = dayOfWeek;
            weeklyScheduleEntry.mTimePairPersist = timePairPersist;

            mScheduleAdapter.notifyItemChanged(mHourMinutePickerPosition);

            mHourMinutePickerPosition = -1;
        }
    };

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

        if (savedInstanceState != null && savedInstanceState.containsKey(SCHEDULE_ENTRY_KEY)) {
            mScheduleEntries = savedInstanceState.getParcelableArrayList(SCHEDULE_ENTRY_KEY);

            mHourMinutePickerPosition = savedInstanceState.getInt(HOUR_MINUTE_PICKER_POSITION_KEY, -2);
            Assert.assertTrue(mHourMinutePickerPosition != -2);
        } else if (args != null && args.containsKey(ROOT_TASK_ID_KEY)) {
            mHourMinutePickerPosition = -1;
        } else {
            mScheduleEntries = new ArrayList<>();
            mScheduleEntries.add(firstScheduleEntry(false));

            mHourMinutePickerPosition = -1;
        }

        mScheduleFab = (FloatingActionButton) view.findViewById(R.id.schedule_fab);
        Assert.assertTrue(mScheduleFab != null);

        mScheduleFab.setOnClickListener(v -> {
            Assert.assertTrue(mScheduleAdapter != null);

            mScheduleAdapter.addScheduleEntry(firstScheduleEntry(true));
        });

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("RepeatingScheduleFragment.onResume");

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

            mFirst = false;

            boolean showDelete = (mData.ScheduleDatas.size() > 1);
            mScheduleEntries = Stream.of(mData.ScheduleDatas)
                    .map(scheduleData -> {
                        switch (scheduleData.getScheduleType()) {
                            case SINGLE:
                                return new SingleScheduleEntry(((ScheduleLoader.SingleScheduleData) scheduleData).Date, ((ScheduleLoader.SingleScheduleData) scheduleData).TimePair, showDelete);
                            case DAILY:
                                return new DailyScheduleEntry(((ScheduleLoader.DailyScheduleData) scheduleData).TimePair, showDelete);
                            case WEEKLY:
                                return new WeeklyScheduleEntry(((ScheduleLoader.WeeklyScheduleData) scheduleData).DayOfWeek, ((ScheduleLoader.WeeklyScheduleData) scheduleData).TimePair, showDelete);
                            default:
                                throw new UnsupportedOperationException();
                        }
                    })
                    .collect(Collectors.toList());
        }

        mScheduleAdapter = new ScheduleAdapter(getContext());
        mScheduleTimes.setAdapter(mScheduleAdapter);

        DailyScheduleDialogFragment dailyScheduleDialogFragment = (DailyScheduleDialogFragment) getChildFragmentManager().findFragmentByTag(DAILY_SCHEDULE_DIALOG_TAG);
        if (dailyScheduleDialogFragment != null)
            dailyScheduleDialogFragment.initialize(mData.CustomTimeDatas, mDailyScheduleDialogListener);

        WeeklyScheduleDialogFragment weeklyScheduleDialogFragment = (WeeklyScheduleDialogFragment) getChildFragmentManager().findFragmentByTag(WEEKLY_SCHEDULE_DIALOG_TAG);
        if (weeklyScheduleDialogFragment != null)
            weeklyScheduleDialogFragment.initialize(data.CustomTimeDatas, mWeeklyScheduleDialogListener);

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Assert.assertTrue(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE);
        Assert.assertTrue(resultCode >= 0);
        Assert.assertTrue(data == null);

        Assert.assertTrue(mHourMinutePickerPosition >= 0);

        if (resultCode > 0) {
            ScheduleEntry scheduleEntry = mScheduleEntries.get(mHourMinutePickerPosition);
            Assert.assertTrue(scheduleEntry != null);

            scheduleEntry.mTimePairPersist.setCustomTimeId(resultCode);
        }

        mHourMinutePickerPosition = -1;
    }

    @Override
    public boolean createRootTask(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        if (mData == null)
            return false;

        List<ScheduleLoader.ScheduleData> scheduleDatas = Stream.of(mScheduleEntries)
                .map(ScheduleEntry::getScheduleData)
                .collect(Collectors.toList());
        Assert.assertTrue(scheduleDatas != null);
        Assert.assertTrue(!scheduleDatas.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).createScheduleRootTask(mData.DataId, name, scheduleDatas);

        TickService.startService(getActivity());

        return true;
    }

    @Override
    public boolean updateRootTask(int rootTaskId, String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        if (mData == null)
            return false;

        List<ScheduleLoader.ScheduleData> scheduleDatas = Stream.of(mScheduleEntries)
                .map(ScheduleEntry::getScheduleData)
                .collect(Collectors.toList());
        Assert.assertTrue(scheduleDatas != null);
        Assert.assertTrue(!scheduleDatas.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).updateScheduleTask(mData.DataId, rootTaskId, name, scheduleDatas);

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

        List<ScheduleLoader.ScheduleData> scheduleDatas = Stream.of(mScheduleEntries)
                .map(ScheduleEntry::getScheduleData)
                .collect(Collectors.toList());
        Assert.assertTrue(scheduleDatas != null);
        Assert.assertTrue(!scheduleDatas.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).createScheduleJoinRootTask(mData.DataId, name, scheduleDatas, joinTaskIds);

        TickService.startService(getActivity());

        return true;
    }

    protected abstract ScheduleEntry firstScheduleEntry(boolean showDelete);

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean dataChanged() {
        Assert.assertTrue(mRootTaskId != null);

        if (mData == null)
            return false;

        Assert.assertTrue(mScheduleAdapter != null);

        Assert.assertTrue(mData.ScheduleDatas != null);

        Multiset<ScheduleLoader.ScheduleData> oldScheduleDatas = HashMultiset.create(mData.ScheduleDatas);

        Multiset<ScheduleLoader.ScheduleData> newScheduleDatas = HashMultiset.create(Stream.of(mScheduleEntries)
                .map(ScheduleEntry::getScheduleData)
                .collect(Collectors.toList()));

        if (!oldScheduleDatas.equals(newScheduleDatas))
            return true;

        return false;
    }

    protected class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleHolder> {
        private final Context mContext;

        public ScheduleAdapter(Context context) {
            Assert.assertTrue(context != null);

            mContext = context;
        }

        @Override
        public ScheduleHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View scheduleRow = LayoutInflater.from(mContext).inflate(R.layout.row_schedule, parent, false);

            TextView scheduleTime = (TextView) scheduleRow.findViewById(R.id.schedule_text);
            Assert.assertTrue(scheduleTime != null);

            ImageView scheduleImage = (ImageView) scheduleRow.findViewById(R.id.schedule_image);
            Assert.assertTrue(scheduleImage != null);

            return new ScheduleHolder(scheduleRow, scheduleTime, scheduleImage);
        }

        @Override
        public void onBindViewHolder(final ScheduleHolder scheduleHolder, int position) {
            final ScheduleEntry scheduleEntry = mScheduleEntries.get(position);
            Assert.assertTrue(scheduleEntry != null);

            scheduleHolder.mScheduleText.setText(scheduleEntry.getText(mData.CustomTimeDatas));

            scheduleHolder.mScheduleText.setOnClickListener(v -> scheduleHolder.onTextClick());

            scheduleHolder.mScheduleImage.setVisibility(scheduleEntry.getShowDelete() ? View.VISIBLE : View.INVISIBLE);

            scheduleHolder.mScheduleImage.setOnClickListener(v -> {
                Assert.assertTrue(mScheduleEntries.size() > 1);
                scheduleHolder.delete();

                if (mScheduleEntries.size() == 1) {
                    mScheduleEntries.get(0).setShowDelete(false);
                    notifyItemChanged(0);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mScheduleEntries.size();
        }

        public void addScheduleEntry(ScheduleEntry scheduleEntry) {
            Assert.assertTrue(scheduleEntry != null);

            int position = mScheduleEntries.size();
            Assert.assertTrue(position > 0);

            if (position == 1) {
                mScheduleEntries.get(0).setShowDelete(true);
                notifyItemChanged(0);
            }

            mScheduleEntries.add(position, scheduleEntry);
            notifyItemInserted(position);
        }

        public class ScheduleHolder extends RecyclerView.ViewHolder {
            public final TextView mScheduleText;
            public final ImageView mScheduleImage;

            public ScheduleHolder(View scheduleRow, TextView scheduleText, ImageView scheduleImage) {
                super(scheduleRow);

                Assert.assertTrue(scheduleText != null);
                Assert.assertTrue(scheduleImage != null);

                mScheduleText = scheduleText;
                mScheduleImage = scheduleImage;
            }

            public void onTextClick() {
                Assert.assertTrue(mData != null);

                mHourMinutePickerPosition = getAdapterPosition();

                ScheduleEntry scheduleEntry = mScheduleEntries.get(mHourMinutePickerPosition);
                Assert.assertTrue(scheduleEntry != null);

                switch (scheduleEntry.getScheduleType()) {
                    case SINGLE:
                        SingleScheduleEntry singleScheduleEntry = (SingleScheduleEntry) scheduleEntry;

                        SingleScheduleDialogFragment singleScheduleDialogFragment = SingleScheduleDialogFragment.newInstance(singleScheduleEntry.mDate, singleScheduleEntry.mTimePairPersist);
                        Assert.assertTrue(singleScheduleDialogFragment != null);

                        singleScheduleDialogFragment.initialize(mData.CustomTimeDatas, mSingleScheduleDialogListener);

                        singleScheduleDialogFragment.show(getChildFragmentManager(), SINGLE_SCHEDULE_DIALOG_TAG);
                        break;
                    case DAILY:
                        DailyScheduleEntry dailyScheduleEntry = (DailyScheduleEntry) scheduleEntry;

                        DailyScheduleDialogFragment dailyScheduleDialogFragment = DailyScheduleDialogFragment.newInstance(dailyScheduleEntry.mTimePairPersist);
                        Assert.assertTrue(dailyScheduleDialogFragment != null);

                        dailyScheduleDialogFragment.initialize(mData.CustomTimeDatas, mDailyScheduleDialogListener);

                        dailyScheduleDialogFragment.show(getChildFragmentManager(), DAILY_SCHEDULE_DIALOG_TAG);
                        break;
                    case WEEKLY:
                        WeeklyScheduleEntry weeklyScheduleEntry = (WeeklyScheduleEntry) scheduleEntry;

                        WeeklyScheduleDialogFragment weeklyScheduleDialogFragment = WeeklyScheduleDialogFragment.newInstance(weeklyScheduleEntry.mDayOfWeek, weeklyScheduleEntry.mTimePairPersist);
                        Assert.assertTrue(weeklyScheduleDialogFragment != null);

                        weeklyScheduleDialogFragment.initialize(mData.CustomTimeDatas, mWeeklyScheduleDialogListener);

                        weeklyScheduleDialogFragment.show(getChildFragmentManager(), WEEKLY_SCHEDULE_DIALOG_TAG);
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }

            public void delete() {
                int position = getAdapterPosition();
                mScheduleEntries.remove(position);
                notifyItemRemoved(position);
            }
        }
    }

    public static abstract class ScheduleEntry implements Parcelable {
        public TimePairPersist mTimePairPersist;

        @Override
        public int describeContents() {
            return 0;
        }

        public abstract ScheduleType getScheduleType();

        public abstract String getText(Map<Integer, ScheduleLoader.CustomTimeData> customTimeDatas);

        public abstract void setShowDelete(boolean delete);

        public abstract boolean getShowDelete();

        public abstract ScheduleLoader.ScheduleData getScheduleData();

        public static final Creator<ScheduleEntry> CREATOR = new Creator<ScheduleEntry>() {
            @Override
            public ScheduleEntry createFromParcel(Parcel in) {
                ScheduleType scheduleType = (ScheduleType) in.readSerializable();
                Assert.assertTrue(scheduleType != null);

                switch (scheduleType) {
                    case SINGLE:
                        return new SingleScheduleEntry(in);
                    case DAILY:
                        return new DailyScheduleEntry(in);
                    case WEEKLY:
                        return new WeeklyScheduleEntry(in);
                    default:
                        throw new UnsupportedOperationException();
                }
            }

            @Override
            public ScheduleEntry[] newArray(int size) {
                return new ScheduleEntry[size];
            }
        };
    }

    public static class SingleScheduleEntry extends RepeatingScheduleFragment.ScheduleEntry {
        public Date mDate;
        private boolean mShowDelete = false;

        public SingleScheduleEntry(Date date, boolean showDelete) {
            Assert.assertTrue(date != null);

            mDate = date;
            mTimePairPersist = new TimePairPersist();
            mShowDelete = showDelete;
        }

        public SingleScheduleEntry(Date date, TimePair timePair, boolean showDelete) {
            Assert.assertTrue(date != null);
            Assert.assertTrue(timePair != null);

            mDate = date;
            mTimePairPersist = new TimePairPersist(timePair);
            mShowDelete = showDelete;
        }

        public SingleScheduleEntry(Parcel parcel) {
            Assert.assertTrue(parcel != null);

            mDate = parcel.readParcelable(Date.class.getClassLoader());
            Assert.assertTrue(mDate != null);

            mTimePairPersist = parcel.readParcelable(TimePairPersist.class.getClassLoader());
            Assert.assertTrue(mTimePairPersist != null);

            int showDeleteInt = parcel.readInt();
            Assert.assertTrue(showDeleteInt == 0 || showDeleteInt == 1);
            mShowDelete = (showDeleteInt == 1);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeSerializable(ScheduleType.SINGLE);

            out.writeParcelable(mDate, 0);
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
            return ScheduleType.SINGLE;
        }

        @Override
        public String getText(Map<Integer, ScheduleLoader.CustomTimeData> customTimeDatas) {
            Assert.assertTrue(customTimeDatas != null);

            if (mTimePairPersist.getCustomTimeId() != null) {
                ScheduleLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePairPersist.getCustomTimeId());
                Assert.assertTrue(customTimeData != null);

                return mDate + ", " + customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDate.getDayOfWeek()) + ")";
            } else {
                return mDate + ", " + mTimePairPersist.getHourMinute().toString();
            }
        }

        @Override
        public ScheduleLoader.ScheduleData getScheduleData() {
            return new ScheduleLoader.SingleScheduleData(mDate, mTimePairPersist.getTimePair());
        }
    }

    public static class DailyScheduleEntry extends RepeatingScheduleFragment.ScheduleEntry {
        private boolean mShowDelete;

        public DailyScheduleEntry(Parcel parcel) {
            Assert.assertTrue(parcel != null);

            mTimePairPersist = parcel.readParcelable(TimePair.class.getClassLoader());
            Assert.assertTrue(mTimePairPersist != null);

            int showDeleteInt = parcel.readInt();
            Assert.assertTrue(showDeleteInt == 0 || showDeleteInt == 1);
            mShowDelete = (showDeleteInt == 1);
        }

        public DailyScheduleEntry(TimePair timePair, boolean showDelete) {
            Assert.assertTrue(timePair != null);

            mTimePairPersist = new TimePairPersist(timePair);
            mShowDelete = showDelete;
        }

        DailyScheduleEntry(boolean showDelete) {
            mTimePairPersist = new TimePairPersist();
            mShowDelete = showDelete;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeSerializable(ScheduleType.DAILY);

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
            return ScheduleType.DAILY;
        }

        @Override
        public String getText(Map<Integer, ScheduleLoader.CustomTimeData> customTimeDatas) {
            Assert.assertTrue(customTimeDatas != null);

            if (mTimePairPersist.getCustomTimeId() != null) {
                ScheduleLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePairPersist.getCustomTimeId());
                Assert.assertTrue(customTimeData != null);

                return customTimeData.Name;
            } else {
                return mTimePairPersist.getHourMinute().toString();
            }
        }

        @Override
        public ScheduleLoader.ScheduleData getScheduleData() {
            return new ScheduleLoader.DailyScheduleData(mTimePairPersist.getTimePair());
        }
    }

    public static class WeeklyScheduleEntry extends RepeatingScheduleFragment.ScheduleEntry {
        public DayOfWeek mDayOfWeek;
        private boolean mShowDelete = false;

        public WeeklyScheduleEntry(DayOfWeek dayOfWeek, boolean showDelete) {
            Assert.assertTrue(dayOfWeek != null);

            mDayOfWeek = dayOfWeek;
            mTimePairPersist = new TimePairPersist();
            mShowDelete = showDelete;
        }

        public WeeklyScheduleEntry(DayOfWeek dayOfWeek, TimePair timePair, boolean showDelete) {
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

        @Override
        public ScheduleLoader.ScheduleData getScheduleData() {
            return new ScheduleLoader.WeeklyScheduleData(mDayOfWeek, mTimePairPersist.getTimePair());
        }
    }
}
