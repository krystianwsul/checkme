package com.example.krystianwsul.organizatortest;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;

public class DailyScheduleFragment extends Fragment {
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

        TimeEntryAdapter timeEntryAdapter = new TimeEntryAdapter(getContext());
        dailyScheduleTimes.setAdapter(timeEntryAdapter);
    }

    private static class TimeEntryAdapter extends RecyclerView.Adapter<TimeHolder> {
        private ArrayList<TimeEntry> mTimeEntries = new ArrayList<>();
        private Context mContext;

        public TimeEntryAdapter(Context context) {
            Assert.assertTrue(context != null);
            mContext = context;

            mTimeEntries.add(new TimeEntry(HourMinute.getNow()));
        }

        @Override
        public TimeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout dailyScheduleRow = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.daily_schedule_row, parent, false);

            TimePickerView dailyScheduleTime = (TimePickerView) dailyScheduleRow.findViewById(R.id.daily_schedule_time);

            return new TimeHolder(dailyScheduleRow, dailyScheduleTime);
        }

        @Override
        public void onBindViewHolder(TimeHolder timeHolder, int position) {
            TimeEntry timeEntry = mTimeEntries.get(position);
            Assert.assertTrue(timeEntry != null);

            timeHolder.mDailyScheduleTime.setOnTimeSelectedListener(new TimePickerView.OnTimeSelectedListener() {
                @Override
                public void onCustomTimeSelected(CustomTime customTime) {

                }

                @Override
                public void onHourMinuteSelected(HourMinute hourMinute) {

                }

                @Override
                public void onHourMinuteClick() {

                }
            });

            if (timeEntry.getCustomTime() != null) {
                Assert.assertTrue(timeEntry.getHourMinute() == null);
                timeHolder.mDailyScheduleTime.setCustomTime(timeEntry.getCustomTime());
            } else {
                Assert.assertTrue(timeEntry.getHourMinute() != null);
                //timeHolder.mDailyScheduleTime.setHourMinute(timeEntry.getHourMinute());
            }
        }

        @Override
        public int getItemCount() {
            return mTimeEntries.size();
        }
    }

    private static class TimeHolder extends RecyclerView.ViewHolder {
        public final LinearLayout mDailyScheduleRow;
        public final TimePickerView mDailyScheduleTime;

        public TimeHolder(LinearLayout dailyScheduleRow, TimePickerView dailyScheduleTime) {
            super(dailyScheduleRow);

            Assert.assertTrue(dailyScheduleTime != null);

            mDailyScheduleRow = dailyScheduleRow;
            mDailyScheduleTime = dailyScheduleTime;
        }
    }

    private static class TimeEntry {
        private CustomTime mCustomTime;
        private HourMinute mHourMinute;

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
    }
}
