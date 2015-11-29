package com.example.krystianwsul.organizatortest;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;

public class ScheduleTimeFragment extends Fragment implements HourMinutePickerFragment.HourMinutePickerFragmentListener {
    private Spinner mCustomTimeView;
    private TextView mNormalTimeView;

    private ArrayAdapter<SpinnerItem> mSpinnerAdapter;
    private OtherSpinnerItem mOtherSpinnerItem;

    private CustomTime mCustomTime;
    private HourMinute mHourMinute;

    private static String CUSTOM_TIME_KEY = "customTimeId";
    private static String HOUR_KEY = "hour";
    private static String MINUTE_KEY = "minute";
    private static String SELECTION_KEY = "selection";

    public static ScheduleTimeFragment getInstance() {
        return new ScheduleTimeFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_time, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        int initialCount = 1;
        if (savedInstanceState != null) {
            int customTimeId = savedInstanceState.getInt(CUSTOM_TIME_KEY, -1);
            int hour = savedInstanceState.getInt(HOUR_KEY, -1);
            int minute = savedInstanceState.getInt(MINUTE_KEY, -1);

            Assert.assertTrue((hour == -1) == (minute == -1));
            Assert.assertTrue((hour == -1) != (customTimeId == -1));

            if (customTimeId != -1) {
                mCustomTime = CustomTimeFactory.getInstance().getCustomTime(customTimeId);
                Assert.assertTrue(mCustomTime != null);

                mHourMinute = null;
            } else {
                mCustomTime = null;
                mHourMinute = new HourMinute(hour, minute);
            }

            int selection = savedInstanceState.getInt(SELECTION_KEY, -1);
            Assert.assertTrue(selection != -1);
            if (selection != 0)
                initialCount = 2;
        } else {
            mCustomTime = null;
            mHourMinute = HourMinute.getNow();
        }

        final int finalCount = initialCount;

        View view = getView();
        Assert.assertTrue(view != null);

        ArrayList<SpinnerItem> spinnerTimes = new ArrayList<>();
        for (CustomTime customTime : CustomTimeFactory.getInstance().getCustomTimes())
            spinnerTimes.add(new TimeSpinnerItem(customTime));
        mOtherSpinnerItem = new OtherSpinnerItem(getActivity());
        spinnerTimes.add(mOtherSpinnerItem);

        mSpinnerAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, spinnerTimes);
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mCustomTimeView = (Spinner) view.findViewById(R.id.schedule_time_customtime);
        mCustomTimeView.setAdapter(mSpinnerAdapter);

        if (savedInstanceState == null)
            mCustomTimeView.setSelection(mSpinnerAdapter.getPosition(mOtherSpinnerItem));

        mCustomTimeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int mInitialCount = finalCount;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SpinnerItem spinnerItem = mSpinnerAdapter.getItem(position);
                Assert.assertTrue(spinnerItem != null);

                if (mInitialCount > 0) {
                    mInitialCount--;
                    return;
                }

                if (spinnerItem == mOtherSpinnerItem) {
                    mCustomTime = null;
                    mHourMinute = HourMinute.getNow();
                } else {
                    TimeSpinnerItem timeSpinnerItem = (TimeSpinnerItem) spinnerItem;
                    mCustomTime = timeSpinnerItem.getCustomTime();
                    mHourMinute = null;
                }

                updateTimeText();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mNormalTimeView = (TextView) view.findViewById(R.id.schedule_time_normaltime);

        final ScheduleTimeFragment scheduleTimeFragment = this;
        mNormalTimeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getChildFragmentManager();
                HourMinutePickerFragment hourMinutePickerFragment = HourMinutePickerFragment.newInstance(scheduleTimeFragment, mHourMinute);
                hourMinutePickerFragment.show(fragmentManager, "time");
            }
        });

        updateTimeText();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCustomTime != null) {
            outState.putInt(CUSTOM_TIME_KEY, mCustomTime.getId());
        } else {
            outState.putInt(HOUR_KEY, mHourMinute.getHour());
            outState.putInt(MINUTE_KEY, mHourMinute.getMinute());
        }

        outState.putInt(SELECTION_KEY, mCustomTimeView.getSelectedItemPosition());
    }

    private void updateTimeText() {
        Assert.assertTrue(mNormalTimeView != null);
        Assert.assertTrue((mCustomTime == null) != (mHourMinute == null));

        if (mCustomTime != null) {
            mNormalTimeView.setVisibility(View.INVISIBLE);
        } else {
            mNormalTimeView.setVisibility(View.VISIBLE);
            mNormalTimeView.setText(mHourMinute.toString());
        }
    }

    @Override
    public void onHourMinutePickerFragmentResult(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);

        mHourMinute = hourMinute;
        updateTimeText();
    }

    private interface SpinnerItem {

    }

    private class TimeSpinnerItem implements SpinnerItem {
        private final CustomTime mCustomTime;

        public TimeSpinnerItem(CustomTime customTime) {
            Assert.assertTrue(customTime != null);
            mCustomTime = customTime;
        }

        public String toString() {
            return mCustomTime.toString();
        }

        public CustomTime getCustomTime() {
            return mCustomTime;
        }
    }

    private class OtherSpinnerItem implements SpinnerItem {
        private final String mOther;

        public OtherSpinnerItem(Context context) {
            Assert.assertTrue(context != null);
            mOther = context.getString(R.string.other);
        }

        public String toString() {
            return mOther;
        }
    }
}
