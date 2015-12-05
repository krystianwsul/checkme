package com.example.krystianwsul.organizatortest;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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

import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class WeeklyScheduleFragment extends Fragment implements HourMinutePickerFragment.HourMinutePickerFragmentListener, ScheduleFragment {
    private int mHourMinutePickerPosition = -1;

    private DayOfWeekTimeEntryAdapter mDayOfWeekTimeEntryAdapter;

    private static final String DATE_TIME_ENTRY_KEY = "dateTimeEntries";
    private static final String HOUR_MINUTE_PICKER_POSITION_KEY = "hourMinutePickerPosition";

    public static WeeklyScheduleFragment newInstance() {
        return new WeeklyScheduleFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_weekly_schedule, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();
        Assert.assertTrue(view != null);
        RecyclerView dailyScheduleTimes = (RecyclerView) view.findViewById(R.id.weekly_schedule_datetimes);
        dailyScheduleTimes.setLayoutManager(new LinearLayoutManager(getContext()));

        if (savedInstanceState != null) {
            List<DayOfWeekTimeEntry> dateTimeEntries = savedInstanceState.getParcelableArrayList(DATE_TIME_ENTRY_KEY);
            mDayOfWeekTimeEntryAdapter = new DayOfWeekTimeEntryAdapter(getContext(), dateTimeEntries);

            mHourMinutePickerPosition = savedInstanceState.getInt(HOUR_MINUTE_PICKER_POSITION_KEY, -2);
            Assert.assertTrue(mHourMinutePickerPosition != -2);
        } else {
            mDayOfWeekTimeEntryAdapter = new DayOfWeekTimeEntryAdapter(getContext());
            mHourMinutePickerPosition = -1;
        }
        dailyScheduleTimes.setAdapter(mDayOfWeekTimeEntryAdapter);

        FloatingActionButton weeklyScheduleFab = (FloatingActionButton) view.findViewById(R.id.weekly_schedule_fab);
        Assert.assertTrue(weeklyScheduleFab != null);

        weeklyScheduleFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDayOfWeekTimeEntryAdapter.addDayOfWeekTimeEntry();
            }
        });
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
    public RootTask createRootTask(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        return TaskFactory.getInstance().createWeeklyScheduleTask(name, mDayOfWeekTimeEntryAdapter.getDayOfWeekTimeEntries());
    }

    private class DayOfWeekTimeEntryAdapter extends RecyclerView.Adapter<DayOfWeekTimeEntryAdapter.DayOfWeekTimeHolder> {
        private final ArrayList<DayOfWeekTimeEntry> mDateTimeEntries;
        private Context mContext;

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

            if (dayOfWeekTimeEntry.getCustomTime() != null) {
                Assert.assertTrue(dayOfWeekTimeEntry.getHourMinute() == null);
                dayOfWeekTimeHolder.mWeeklyScheduleTime.setCustomTime(dayOfWeekTimeEntry.getCustomTime());
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

        public int indexOf(DayOfWeekTimeEntry dayOfWeekTimeEntry) {
            Assert.assertTrue(dayOfWeekTimeEntry != null);
            Assert.assertTrue(mDateTimeEntries.contains(dayOfWeekTimeEntry));

            return mDateTimeEntries.indexOf(dayOfWeekTimeEntry);
        }

        public ArrayList<DayOfWeekTimeEntry> getDayOfWeekTimeEntries() {
            return mDateTimeEntries;
        }

        public class DayOfWeekTimeHolder extends RecyclerView.ViewHolder {
            public final RelativeLayout mWeeklyScheduleRow;
            public final Spinner mWeeklyScheduleDay;
            public final TimePickerView mWeeklyScheduleTime;
            public final ImageView mWeeklyScheduleImage;

            public DayOfWeekTimeHolder(RelativeLayout weeklyScheduleRow, Spinner weeklyScheduleDay, TimePickerView weeklyScheduleTime, ImageView weeklyScheduleImage) {
                super(weeklyScheduleRow);

                Assert.assertTrue(weeklyScheduleDay != null);
                Assert.assertTrue(weeklyScheduleTime != null);
                Assert.assertTrue(weeklyScheduleImage != null);

                mWeeklyScheduleDay = weeklyScheduleDay;
                mWeeklyScheduleRow = weeklyScheduleRow;
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
        private DayOfWeek mDayOfWeek;
        private CustomTime mCustomTime;
        private HourMinute mHourMinute;
        private boolean mShowDelete = false;

        public DayOfWeekTimeEntry(DayOfWeek dayOfWeek, CustomTime customTime, boolean showDelete) {
            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(customTime != null);

            mDayOfWeek = dayOfWeek;
            setCustomTime(customTime);
            mShowDelete = showDelete;
        }

        public DayOfWeekTimeEntry(DayOfWeek dayOfWeek, HourMinute hourMinute, boolean showDelete) {
            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(hourMinute != null);

            mDayOfWeek = dayOfWeek;
            setHourMinute(hourMinute);
            mShowDelete = showDelete;
        }

        public DayOfWeek getDayOfWeek() {
            return mDayOfWeek;
        }

        public CustomTime getCustomTime() {
            return mCustomTime;
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

            mCustomTime = customTime;
            mHourMinute = null;
        }

        public void setHourMinute(HourMinute hourMinute) {
            Assert.assertTrue(hourMinute != null);

            mHourMinute = hourMinute;
            mCustomTime = null;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            Assert.assertTrue((mCustomTime == null) != (mHourMinute == null));

            out.writeSerializable(mDayOfWeek);

            if (mCustomTime != null)
                out.writeInt(mCustomTime.getId());
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

        public static final Parcelable.Creator<DayOfWeekTimeEntry> CREATOR = new Parcelable.Creator<DayOfWeekTimeEntry>() {
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
                    return new DayOfWeekTimeEntry(dayOfWeek, CustomTimeFactory.getInstance().getCustomTime(customTimeId), showDelete);
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
