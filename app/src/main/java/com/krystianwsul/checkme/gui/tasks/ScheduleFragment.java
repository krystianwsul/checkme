package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ScheduleFragment extends Fragment implements LoaderManager.LoaderCallbacks<ScheduleLoader.Data> {
    static final String SCHEDULE_HINT_KEY = "scheduleHint";
    static final String ROOT_TASK_ID_KEY = "rootTaskId";

    private static final String HOUR_MINUTE_PICKER_POSITION_KEY = "hourMinutePickerPosition";

    private static final String SCHEDULE_ENTRY_KEY = "scheduleEntries";

    private static final String SCHEDULE_DIALOG_TAG = "scheduleDialog";

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

    private final ScheduleDialogFragment.ScheduleDialogListener mScheduleDialogListener = new ScheduleDialogFragment.ScheduleDialogListener() {
        @Override
        public void onScheduleDialogResult(@NonNull ScheduleDialogFragment.ScheduleDialogData scheduleDialogData) {
            Assert.assertTrue(mHourMinutePickerPosition != -1);
            Assert.assertTrue(mData != null);

            ScheduleEntry scheduleEntry = mScheduleEntries.get(mHourMinutePickerPosition);
            Assert.assertTrue(scheduleEntry != null);

            scheduleEntry.mDate = scheduleDialogData.mDate;
            scheduleEntry.mDayOfWeek = scheduleDialogData.mDayOfWeek;
            scheduleEntry.mTimePairPersist = scheduleDialogData.mTimePairPersist;
            scheduleEntry.mScheduleType = scheduleDialogData.mScheduleType;

            mScheduleAdapter.notifyItemChanged(mHourMinutePickerPosition);

            mHourMinutePickerPosition = -1;
        }
    };

    public static ScheduleFragment newInstance() {
        return new ScheduleFragment();
    }

    @NonNull
    public static ScheduleFragment newInstance(CreateTaskActivity.ScheduleHint scheduleHint) {
        Assert.assertTrue(scheduleHint != null);

        ScheduleFragment scheduleFragment = new ScheduleFragment();

        Bundle args = new Bundle();
        args.putParcelable(SCHEDULE_HINT_KEY, scheduleHint);
        scheduleFragment.setArguments(args);

        return scheduleFragment;
    }

    @NonNull
    public static ScheduleFragment newInstance(int rootTaskId) {
        ScheduleFragment scheduleFragment = new ScheduleFragment();

        Bundle args = new Bundle();
        args.putInt(ROOT_TASK_ID_KEY, rootTaskId);

        scheduleFragment.setArguments(args);
        return scheduleFragment;
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

        if (savedInstanceState != null && savedInstanceState.containsKey(SCHEDULE_ENTRY_KEY)) {
            mScheduleEntries = savedInstanceState.getParcelableArrayList(SCHEDULE_ENTRY_KEY);

            mHourMinutePickerPosition = savedInstanceState.getInt(HOUR_MINUTE_PICKER_POSITION_KEY, -2);
            Assert.assertTrue(mHourMinutePickerPosition != -2);
        } else if (args != null && args.containsKey(ROOT_TASK_ID_KEY)) {
            mHourMinutePickerPosition = -1;
        } else {
            mScheduleEntries = new ArrayList<>();
            mScheduleEntries.add(firstScheduleEntry());

            mHourMinutePickerPosition = -1;
        }

        mScheduleFab = (FloatingActionButton) view.findViewById(R.id.schedule_fab);
        Assert.assertTrue(mScheduleFab != null);

        mScheduleFab.setOnClickListener(v -> {
            Assert.assertTrue(mScheduleAdapter != null);

            ((CreateTaskActivity) getActivity()).clearParent();
            mScheduleAdapter.addScheduleEntry(firstScheduleEntry());
        });

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("ScheduleFragment.onResume");

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

            mScheduleEntries = Stream.of(mData.ScheduleDatas)
                    .map(scheduleData -> {
                        switch (scheduleData.getScheduleType()) {
                            case SINGLE:
                                return new ScheduleEntry(((ScheduleLoader.SingleScheduleData) scheduleData).Date, ((ScheduleLoader.SingleScheduleData) scheduleData).TimePair);
                            case DAILY:
                                return new ScheduleEntry(((ScheduleLoader.DailyScheduleData) scheduleData).TimePair);
                            case WEEKLY:
                                return new ScheduleEntry(((ScheduleLoader.WeeklyScheduleData) scheduleData).DayOfWeek, ((ScheduleLoader.WeeklyScheduleData) scheduleData).TimePair);
                            default:
                                throw new UnsupportedOperationException();
                        }
                    })
                    .collect(Collectors.toList());
        }

        ScheduleDialogFragment singleDialogFragment = (ScheduleDialogFragment) getChildFragmentManager().findFragmentByTag(SCHEDULE_DIALOG_TAG);
        if (singleDialogFragment != null)
            singleDialogFragment.initialize(mData.CustomTimeDatas, mScheduleDialogListener);

        mScheduleAdapter = new ScheduleAdapter(getContext());
        mScheduleTimes.setAdapter(mScheduleAdapter);

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

    private ScheduleEntry firstScheduleEntry() {
        if (mScheduleHint != null) {
            if (mScheduleHint.mTimePair != null) {
                return new ScheduleEntry(mScheduleHint.mDate, mScheduleHint.mTimePair);
            } else {
                return new ScheduleEntry(mScheduleHint.mDate);
            }
        } else {
            return new ScheduleEntry(Date.today());
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean dataChanged() {
        if (mData == null)
            return false;

        Assert.assertTrue(mScheduleAdapter != null);

        Multiset<ScheduleLoader.ScheduleData> oldScheduleDatas;
        if (mData.ScheduleDatas != null) {
            oldScheduleDatas = HashMultiset.create(mData.ScheduleDatas);
        } else {
            oldScheduleDatas = HashMultiset.create(Collections.singletonList(firstScheduleEntry().getScheduleData()));
        }

        Multiset<ScheduleLoader.ScheduleData> newScheduleDatas = HashMultiset.create(Stream.of(mScheduleEntries)
                .map(ScheduleEntry::getScheduleData)
                .collect(Collectors.toList()));

        if (!oldScheduleDatas.equals(newScheduleDatas))
            return true;

        return false;
    }

    public void clearSchedules() {
        int count = mScheduleEntries.size();

        mScheduleEntries = new ArrayList<>();
        mScheduleAdapter.notifyItemRangeRemoved(0, count);
    }

    public boolean isEmpty() {
        return mScheduleEntries.isEmpty();
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

            scheduleHolder.mScheduleImage.setOnClickListener(v -> scheduleHolder.delete());
        }

        @Override
        public int getItemCount() {
            return mScheduleEntries.size();
        }

        public void addScheduleEntry(ScheduleEntry scheduleEntry) {
            Assert.assertTrue(scheduleEntry != null);

            int position = mScheduleEntries.size();

            mScheduleEntries.add(scheduleEntry);
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

                ScheduleDialogFragment scheduleDialogFragment = ScheduleDialogFragment.newInstance(scheduleEntry.getScheduleDialogData(mScheduleHint));
                scheduleDialogFragment.initialize(mData.CustomTimeDatas, mScheduleDialogListener);
                scheduleDialogFragment.show(getChildFragmentManager(), SCHEDULE_DIALOG_TAG);
            }

            public void delete() {
                int position = getAdapterPosition();
                mScheduleEntries.remove(position);
                notifyItemRemoved(position);
            }
        }
    }

    public static class ScheduleEntry implements Parcelable {
        public Date mDate;
        public DayOfWeek mDayOfWeek;
        public TimePairPersist mTimePairPersist;
        public ScheduleType mScheduleType;

        public ScheduleEntry(@NonNull Date date) {
            mDate = date;
            mDayOfWeek = mDate.getDayOfWeek();
            mTimePairPersist = new TimePairPersist();
            mScheduleType = ScheduleType.SINGLE;
        }

        public ScheduleEntry(@NonNull Date date, @NonNull TimePair timePair) {
            mDate = date;
            mDayOfWeek = mDate.getDayOfWeek();
            mTimePairPersist = new TimePairPersist(timePair);
            mScheduleType = ScheduleType.SINGLE;
        }

        public ScheduleEntry(@NonNull Date date, @NonNull DayOfWeek dayOfWeek, @NonNull TimePairPersist timePairPersist, @NonNull ScheduleType scheduleType) {
            mDate = date;
            mDayOfWeek = dayOfWeek;
            mTimePairPersist = timePairPersist;
            mScheduleType = scheduleType;
        }

        public ScheduleEntry(@NonNull TimePair timePair) {
            mDate = Date.today();
            mDayOfWeek = mDate.getDayOfWeek();
            mTimePairPersist = new TimePairPersist(timePair);
            mScheduleType = ScheduleType.DAILY;
        }

        public ScheduleEntry() {
            mDate = Date.today();
            mDayOfWeek = mDate.getDayOfWeek();
            mTimePairPersist = new TimePairPersist();
            mScheduleType = ScheduleType.DAILY;
        }

        public ScheduleEntry(@NonNull DayOfWeek dayOfWeek) {
            mDate = Date.today();
            mDayOfWeek = dayOfWeek;
            mTimePairPersist = new TimePairPersist();
            mScheduleType = ScheduleType.WEEKLY;
        }

        public ScheduleEntry(@NonNull DayOfWeek dayOfWeek, @NonNull TimePair timePair) {
            mDate = Date.today();
            mDayOfWeek = dayOfWeek;
            mTimePairPersist = new TimePairPersist(timePair);
            mScheduleType = ScheduleType.WEEKLY;
        }

        @NonNull
        public String getText(@NonNull Map<Integer, ScheduleLoader.CustomTimeData> customTimeDatas) {
            switch (mScheduleType) {
                case SINGLE:
                    if (mTimePairPersist.getCustomTimeId() != null) {
                        ScheduleLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePairPersist.getCustomTimeId());
                        Assert.assertTrue(customTimeData != null);

                        return mDate + ", " + customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDate.getDayOfWeek()) + ")";
                    } else {
                        return mDate + ", " + mTimePairPersist.getHourMinute().toString();
                    }
                case DAILY:
                    if (mTimePairPersist.getCustomTimeId() != null) {
                        ScheduleLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePairPersist.getCustomTimeId());
                        Assert.assertTrue(customTimeData != null);

                        return customTimeData.Name;
                    } else {
                        return mTimePairPersist.getHourMinute().toString();
                    }
                case WEEKLY:
                    if (mTimePairPersist.getCustomTimeId() != null) {
                        ScheduleLoader.CustomTimeData customTimeData = customTimeDatas.get(mTimePairPersist.getCustomTimeId());
                        Assert.assertTrue(customTimeData != null);

                        return mDayOfWeek + ", " + customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDayOfWeek) + ")";
                    } else {
                        return mDayOfWeek + ", " + mTimePairPersist.getHourMinute().toString();
                    }
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @NonNull
        public ScheduleLoader.ScheduleData getScheduleData() {
            switch (mScheduleType) {
                case SINGLE:
                    return new ScheduleLoader.SingleScheduleData(mDate, mTimePairPersist.getTimePair());
                case DAILY:
                    return new ScheduleLoader.DailyScheduleData(mTimePairPersist.getTimePair());
                case WEEKLY:
                    return new ScheduleLoader.WeeklyScheduleData(mDayOfWeek, mTimePairPersist.getTimePair());
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @SuppressWarnings("unused")
        @NonNull
        public ScheduleDialogFragment.ScheduleDialogData getScheduleDialogData(CreateTaskActivity.ScheduleHint scheduleHint) {
            switch (mScheduleType) {
                case SINGLE: {
                    return new ScheduleDialogFragment.ScheduleDialogData(mDate, mDate.getDayOfWeek(), mTimePairPersist, ScheduleType.SINGLE);
                }
                case DAILY: {
                    Date date = (scheduleHint != null ? scheduleHint.mDate : Date.today());

                    return new ScheduleDialogFragment.ScheduleDialogData(date, date.getDayOfWeek(), mTimePairPersist, ScheduleType.DAILY);
                }
                case WEEKLY: {
                    Date date = (scheduleHint != null ? scheduleHint.mDate : Date.today());

                    return new ScheduleDialogFragment.ScheduleDialogData(date, mDayOfWeek, mTimePairPersist, ScheduleType.WEEKLY);
                }
                default: {
                    throw new UnsupportedOperationException();
                }
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeParcelable(mDate, 0);
            parcel.writeSerializable(mDayOfWeek);
            parcel.writeParcelable(mTimePairPersist, 0);
            parcel.writeSerializable(mScheduleType);
        }

        @SuppressWarnings("unused")
        public static final Creator<ScheduleEntry> CREATOR = new Creator<ScheduleEntry>() {
            @Override
            public ScheduleEntry createFromParcel(Parcel in) {
                Date date = in.readParcelable(Date.class.getClassLoader());
                Assert.assertTrue(date != null);

                DayOfWeek dayOfWeek = (DayOfWeek) in.readSerializable();
                Assert.assertTrue(dayOfWeek != null);

                TimePairPersist timePairPersist = in.readParcelable(TimePairPersist.class.getClassLoader());
                Assert.assertTrue(timePairPersist != null);

                ScheduleType scheduleType = (ScheduleType) in.readSerializable();
                Assert.assertTrue(scheduleType != null);

                return new ScheduleEntry(date, dayOfWeek, timePairPersist, scheduleType);
            }

            @Override
            public ScheduleEntry[] newArray(int size) {
                return new ScheduleEntry[size];
            }
        };
    }
}
