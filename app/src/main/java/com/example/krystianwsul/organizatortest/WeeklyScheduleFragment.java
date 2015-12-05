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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.schedules.Schedule;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class WeeklyScheduleFragment extends Fragment implements DatePickerFragment.DatePickerFragmentListener, HourMinutePickerFragment.HourMinutePickerFragmentListener, ScheduleFragment {
    private int mHourMinutePickerPosition = -1;
    private int mDatePickerPosition = -1;

    private DateTimeEntryAdapter mDateTimeEntryAdapter;

    private static final String DATE_TIME_ENTRY_KEY = "dateTimeEntries";
    private static final String HOUR_MINUTE_PICKER_POSITION_KEY = "hourMinutePickerPosition";
    private static final String DATE_PICKER_POSITION_KEY = "datePickerPosition";

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
            List<DateTimeEntry> dateTimeEntries = savedInstanceState.getParcelableArrayList(DATE_TIME_ENTRY_KEY);
            mDateTimeEntryAdapter = new DateTimeEntryAdapter(getContext(), dateTimeEntries);

            mHourMinutePickerPosition = savedInstanceState.getInt(HOUR_MINUTE_PICKER_POSITION_KEY, -2);
            Assert.assertTrue(mHourMinutePickerPosition != -2);

            mDatePickerPosition = savedInstanceState.getInt(DATE_PICKER_POSITION_KEY, -2);
            Assert.assertTrue(mDatePickerPosition != -2);
        } else {
            mDateTimeEntryAdapter = new DateTimeEntryAdapter(getContext());
            mHourMinutePickerPosition = -1;
            mDatePickerPosition = -1;
        }
        dailyScheduleTimes.setAdapter(mDateTimeEntryAdapter);

        FloatingActionButton weeklyScheduleFab = (FloatingActionButton) view.findViewById(R.id.weekly_schedule_fab);
        Assert.assertTrue(weeklyScheduleFab != null);

        weeklyScheduleFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDateTimeEntryAdapter.addDateTimeEntry();
            }
        });
    }

    public void onDatePickerFragmentResult(Date date) {
        Assert.assertTrue(date != null);
        Assert.assertTrue(mDatePickerPosition != -1);

        DateTimeEntry dateTimeEntry = mDateTimeEntryAdapter.getDateTimeEntry(mDatePickerPosition);

        dateTimeEntry.setDate(date);
        mDateTimeEntryAdapter.notifyItemChanged(mDatePickerPosition);

        mDatePickerPosition = -1;
    }

    public void onHourMinutePickerFragmentResult(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);
        Assert.assertTrue(mHourMinutePickerPosition != -1);

        DateTimeEntry dateTimeEntry = mDateTimeEntryAdapter.getDateTimeEntry(mHourMinutePickerPosition);

        dateTimeEntry.setHourMinute(hourMinute);
        mDateTimeEntryAdapter.notifyItemChanged(mHourMinutePickerPosition);

        mHourMinutePickerPosition = -1;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(DATE_TIME_ENTRY_KEY, mDateTimeEntryAdapter.getDateTimeEntries());
        outState.putInt(HOUR_MINUTE_PICKER_POSITION_KEY, mHourMinutePickerPosition);
        outState.putInt(DATE_PICKER_POSITION_KEY, mDatePickerPosition);
    }

    @Override
    public Schedule getSchedule() {
        throw new UnsupportedOperationException();
    }

    private class DateTimeEntryAdapter extends RecyclerView.Adapter<DateTimeEntryAdapter.DateTimeHolder> {
        private final ArrayList<DateTimeEntry> mDateTimeEntries;
        private Context mContext;

        public DateTimeEntryAdapter(Context context) {
            Assert.assertTrue(context != null);

            mContext = context;
            mDateTimeEntries = new ArrayList<>();
            mDateTimeEntries.add(new DateTimeEntry(Date.today(), HourMinute.getNow(), false));
        }

        public DateTimeEntryAdapter(Context context, List<DateTimeEntry> dateTimeEntries) {
            Assert.assertTrue(context != null);
            Assert.assertTrue(dateTimeEntries != null);
            Assert.assertTrue(!dateTimeEntries.isEmpty());

            mContext = context;
            mDateTimeEntries = new ArrayList<>(dateTimeEntries);
        }

        public DateTimeEntry getDateTimeEntry(int position) {
            Assert.assertTrue(position >= 0);
            Assert.assertTrue(position < getItemCount());

            return mDateTimeEntries.get(position);
        }

        @Override
        public DateTimeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RelativeLayout weeklyScheduleRow = (RelativeLayout) LayoutInflater.from(mContext).inflate(R.layout.weekly_schedule_row, parent, false);

            TextView weeklyScheduleDate = (TextView) weeklyScheduleRow.findViewById(R.id.weekly_schedule_date);
            TimePickerView weeklyScheduleTime = (TimePickerView) weeklyScheduleRow.findViewById(R.id.weekly_schedule_time);
            ImageView weeklyScheduleImage = (ImageView) weeklyScheduleRow.findViewById(R.id.weekly_schedule_image);

            return new DateTimeHolder(weeklyScheduleRow, weeklyScheduleDate, weeklyScheduleTime, weeklyScheduleImage);
        }

        @Override
        public void onBindViewHolder(final DateTimeHolder dateTimeHolder, int position) {
            final DateTimeEntry dateTimeEntry = mDateTimeEntries.get(position);
            Assert.assertTrue(dateTimeEntry != null);

            dateTimeHolder.mWeeklyScheduleDate.setText(dateTimeEntry.getDate().getDisplayText(getContext()));

            dateTimeHolder.mWeeklyScheduleDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dateTimeHolder.onDateClick();
                }
            });

            if (dateTimeEntry.getCustomTime() != null) {
                Assert.assertTrue(dateTimeEntry.getHourMinute() == null);
                dateTimeHolder.mWeeklyScheduleTime.setCustomTime(dateTimeEntry.getCustomTime());
            } else {
                Assert.assertTrue(dateTimeEntry.getHourMinute() != null);
                dateTimeHolder.mWeeklyScheduleTime.setHourMinute(dateTimeEntry.getHourMinute());
            }

            dateTimeHolder.mWeeklyScheduleTime.setOnTimeSelectedListener(new TimePickerView.OnTimeSelectedListener() {
                @Override
                public void onCustomTimeSelected(CustomTime customTime) {
                    dateTimeEntry.setCustomTime(customTime);
                }

                @Override
                public void onHourMinuteSelected(HourMinute hourMinute) {
                    dateTimeEntry.setHourMinute(hourMinute);
                }

                @Override
                public void onHourMinuteClick() {
                    dateTimeHolder.onHourMinuteClick();
                }
            });

            dateTimeHolder.mWeeklyScheduleImage.setVisibility(dateTimeEntry.getShowDelete() ? View.VISIBLE : View.INVISIBLE);

            dateTimeHolder.mWeeklyScheduleImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Assert.assertTrue(mDateTimeEntries.size() > 1);
                    dateTimeHolder.delete();

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

        public void addDateTimeEntry() {
            int position = mDateTimeEntries.size();
            Assert.assertTrue(position > 0);

            if (position == 1) {
                mDateTimeEntries.get(0).setShowDelete(true);
                notifyItemChanged(0);
            }

            DateTimeEntry dateTimeEntry = new DateTimeEntry(Date.today(), HourMinute.getNow(), true);
            mDateTimeEntries.add(position, dateTimeEntry);
            notifyItemInserted(position);
        }

        public int indexOf(DateTimeEntry dateTimeEntry) {
            Assert.assertTrue(dateTimeEntry != null);
            Assert.assertTrue(mDateTimeEntries.contains(dateTimeEntry));

            return mDateTimeEntries.indexOf(dateTimeEntry);
        }

        public ArrayList<DateTimeEntry> getDateTimeEntries() {
            return mDateTimeEntries;
        }

        public class DateTimeHolder extends RecyclerView.ViewHolder {
            public final RelativeLayout mWeeklyScheduleRow;
            public final TextView mWeeklyScheduleDate;
            public final TimePickerView mWeeklyScheduleTime;
            public final ImageView mWeeklyScheduleImage;

            public DateTimeHolder(RelativeLayout weeklyScheduleRow, TextView weeklyScheduleDate, TimePickerView weeklyScheduleTime, ImageView weeklyScheduleImage) {
                super(weeklyScheduleRow);

                Assert.assertTrue(weeklyScheduleDate != null);
                Assert.assertTrue(weeklyScheduleTime != null);
                Assert.assertTrue(weeklyScheduleImage != null);

                mWeeklyScheduleDate = weeklyScheduleDate;
                mWeeklyScheduleRow = weeklyScheduleRow;
                mWeeklyScheduleTime = weeklyScheduleTime;
                mWeeklyScheduleImage = weeklyScheduleImage;
            }

            public void onDateClick() {
                mDatePickerPosition = getAdapterPosition();
                DateTimeEntry dateTimeEntry = mDateTimeEntries.get(mDatePickerPosition);
                Assert.assertTrue(dateTimeEntry != null);

                FragmentManager fragmentManager = getChildFragmentManager();
                DatePickerFragment datePickerFragment = DatePickerFragment.newInstance(getActivity(), dateTimeEntry.getDate());
                datePickerFragment.show(fragmentManager, "date");
            }

            public void onHourMinuteClick() {
                mHourMinutePickerPosition = getAdapterPosition();
                DateTimeEntry dateTimeEntry = mDateTimeEntries.get(mHourMinutePickerPosition);
                Assert.assertTrue(dateTimeEntry != null);

                FragmentManager fragmentManager = getChildFragmentManager();
                HourMinutePickerFragment hourMinutePickerFragment = HourMinutePickerFragment.newInstance(getActivity(), dateTimeEntry.getHourMinute());
                hourMinutePickerFragment.show(fragmentManager, "time");
            }

            public void delete() {
                int position = getAdapterPosition();
                mDateTimeEntries.remove(position);
                notifyItemRemoved(position);
            }
        }
    }

    private static class DateTimeEntry implements Parcelable {
        private Date mDate;
        private CustomTime mCustomTime;
        private HourMinute mHourMinute;
        private boolean mShowDelete = false;

        public DateTimeEntry(Date date, CustomTime customTime, boolean showDelete) {
            Assert.assertTrue(date != null);
            Assert.assertTrue(customTime != null);

            mDate = date;
            setCustomTime(customTime);
            mShowDelete = showDelete;
        }

        public DateTimeEntry(Date date, HourMinute hourMinute, boolean showDelete) {
            Assert.assertTrue(date != null);
            Assert.assertTrue(hourMinute != null);

            mDate = date;
            setHourMinute(hourMinute);
            mShowDelete = showDelete;
        }

        public Date getDate() {
            return mDate;
        }

        public CustomTime getCustomTime() {
            return mCustomTime;
        }

        public HourMinute getHourMinute() {
            return mHourMinute;
        }

        public void setDate(Date date) {
            Assert.assertTrue(date != null);
            mDate = date;
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

            out.writeInt(mDate.getYear());
            out.writeInt(mDate.getMonth());
            out.writeInt(mDate.getDay());

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

        public static final Parcelable.Creator<DateTimeEntry> CREATOR = new Parcelable.Creator<DateTimeEntry>() {
            public DateTimeEntry createFromParcel(Parcel in) {
                int year = in.readInt();
                int month = in.readInt();
                int day = in.readInt();
                int customTimeId = in.readInt();
                int hour = in.readInt();
                int minute = in.readInt();
                int showDeleteInt = in.readInt();
                Assert.assertTrue(showDeleteInt == 0 || showDeleteInt == 1);
                boolean showDelete = (showDeleteInt == 1);

                Assert.assertTrue((hour == -1) == (minute == -1));
                Assert.assertTrue((hour == -1) != (customTimeId == -1));

                Date date = new Date(year, month, day);

                if (customTimeId != -1)
                    return new DateTimeEntry(date, CustomTimeFactory.getInstance().getCustomTime(customTimeId), showDelete);
                else
                    return new DateTimeEntry(date, new HourMinute(hour, minute), showDelete);
            }

            public DateTimeEntry[] newArray(int size) {
                return new DateTimeEntry[size];
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
