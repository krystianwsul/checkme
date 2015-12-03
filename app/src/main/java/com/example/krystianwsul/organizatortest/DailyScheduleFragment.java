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

import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class DailyScheduleFragment extends Fragment implements HourMinutePickerFragment.HourMinutePickerFragmentListener {
    private TimeEntry mPickerTimeEntry;

    private TimeEntryAdapter mTimeEntryAdapter;

    private static final String TIME_ENTRY_KEY = "timeEntries";

    public static DailyScheduleFragment newInstance() {
        return new DailyScheduleFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_daily_schedule, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();
        Assert.assertTrue(view != null);
        RecyclerView dailyScheduleTimes = (RecyclerView) view.findViewById(R.id.daily_schedule_times);
        dailyScheduleTimes.setLayoutManager(new LinearLayoutManager(getContext()));

        if (savedInstanceState != null) {
            List<TimeEntry> timeEntries = savedInstanceState.getParcelableArrayList(TIME_ENTRY_KEY);
            mTimeEntryAdapter = new TimeEntryAdapter(getContext(), timeEntries);
        } else {
            mTimeEntryAdapter = new TimeEntryAdapter(getContext());
        }
        dailyScheduleTimes.setAdapter(mTimeEntryAdapter);

        FloatingActionButton dailyScheduleFab = (FloatingActionButton) view.findViewById(R.id.daily_schedule_fab);
        Assert.assertTrue(dailyScheduleFab != null);

        dailyScheduleFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTimeEntryAdapter.addTimeEntry();
            }
        });
    }

    public void onHourMinutePickerFragmentResult(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);
        Assert.assertTrue(mPickerTimeEntry != null);

        mPickerTimeEntry.setHourMinute(hourMinute);
        mTimeEntryAdapter.notifyItemChanged(mTimeEntryAdapter.indexOf(mPickerTimeEntry));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(TIME_ENTRY_KEY, mTimeEntryAdapter.getTimeEntries());
    }

    private class TimeEntryAdapter extends RecyclerView.Adapter<TimeEntryAdapter.TimeHolder> {
        private final ArrayList<TimeEntry> mTimeEntries;
        private Context mContext;

        public TimeEntryAdapter(Context context) {
            Assert.assertTrue(context != null);

            mContext = context;
            mTimeEntries = new ArrayList<>();
            mTimeEntries.add(new TimeEntry(HourMinute.getNow()));
            if (mTimeEntries.size() > 1)
                for (TimeEntry timeEntry : mTimeEntries)
                    timeEntry.setShowDelete(true);
        }

        public TimeEntryAdapter(Context context, List<TimeEntry> timeEntries) {
            Assert.assertTrue(context != null);
            Assert.assertTrue(timeEntries != null);
            Assert.assertTrue(!timeEntries.isEmpty());

            mContext = context;
            mTimeEntries = new ArrayList<>(timeEntries);
        }

        @Override
        public TimeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RelativeLayout dailyScheduleRow = (RelativeLayout) LayoutInflater.from(mContext).inflate(R.layout.daily_schedule_row, parent, false);

            TimePickerView dailyScheduleTime = (TimePickerView) dailyScheduleRow.findViewById(R.id.daily_schedule_time);
            ImageView dailyScheduleImage = (ImageView) dailyScheduleRow.findViewById(R.id.daily_schedule_image);

            return new TimeHolder(dailyScheduleRow, dailyScheduleTime, dailyScheduleImage);
        }

        @Override
        public void onBindViewHolder(final TimeHolder timeHolder, int position) {
            final TimeEntry timeEntry = mTimeEntries.get(position);
            Assert.assertTrue(timeEntry != null);

            timeHolder.mDailyScheduleTime.setOnTimeSelectedListener(new TimePickerView.OnTimeSelectedListener() {
                @Override
                public void onCustomTimeSelected(CustomTime customTime) {
                    timeEntry.setCustomTime(customTime);
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

            if (timeEntry.getCustomTime() != null) {
                Assert.assertTrue(timeEntry.getHourMinute() == null);
                timeHolder.mDailyScheduleTime.setCustomTime(timeEntry.getCustomTime());
            } else {
                Assert.assertTrue(timeEntry.getHourMinute() != null);
                timeHolder.mDailyScheduleTime.setHourMinute(timeEntry.getHourMinute());
            }

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

            TimeEntry timeEntry = new TimeEntry(HourMinute.getNow());
            timeEntry.setShowDelete(true);
            mTimeEntries.add(position, timeEntry);
            notifyItemInserted(position);
        }

        public int indexOf(TimeEntry timeEntry) {
            Assert.assertTrue(timeEntry != null);
            Assert.assertTrue(mTimeEntries.contains(timeEntry));

            return mTimeEntries.indexOf(timeEntry);
        }

        public ArrayList<TimeEntry> getTimeEntries() {
            return mTimeEntries;
        }

        public class TimeHolder extends RecyclerView.ViewHolder {
            public final RelativeLayout mDailyScheduleRow;
            public final TimePickerView mDailyScheduleTime;
            public final ImageView mDailyScheduleImage;

            public TimeHolder(RelativeLayout dailyScheduleRow, TimePickerView dailyScheduleTime, ImageView dailyScheduleImage) {
                super(dailyScheduleRow);

                Assert.assertTrue(dailyScheduleTime != null);
                Assert.assertTrue(dailyScheduleImage != null);

                mDailyScheduleRow = dailyScheduleRow;
                mDailyScheduleTime = dailyScheduleTime;
                mDailyScheduleImage = dailyScheduleImage;
            }

            public void onHourMinuteClick() {
                int position = getAdapterPosition();
                TimeEntry timeEntry = mTimeEntries.get(position);
                Assert.assertTrue(timeEntry != null);

                mPickerTimeEntry = timeEntry;

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

    private static class TimeEntry implements Parcelable {
        private CustomTime mCustomTime;
        private HourMinute mHourMinute;
        private boolean mShowDelete = false;

        public TimeEntry(CustomTime customTime) {
            setCustomTime(customTime);
        }

        public TimeEntry(HourMinute hourMinute) {
            setHourMinute(hourMinute);
        }

        public CustomTime getCustomTime() {
            return mCustomTime;
        }

        public HourMinute getHourMinute() {
            return mHourMinute;
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
        }

        public static final Parcelable.Creator<TimeEntry> CREATOR = new Parcelable.Creator<TimeEntry>() {
            public TimeEntry createFromParcel(Parcel in) {
                int customTimeId = in.readInt();
                int hour = in.readInt();
                int minute = in.readInt();

                Assert.assertTrue((hour == -1) == (minute == -1));
                Assert.assertTrue((hour == -1) != (customTimeId == -1));

                if (customTimeId != -1)
                    return new TimeEntry(CustomTimeFactory.getInstance().getCustomTime(customTimeId));
                else
                    return new TimeEntry(new HourMinute(hour, minute));
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