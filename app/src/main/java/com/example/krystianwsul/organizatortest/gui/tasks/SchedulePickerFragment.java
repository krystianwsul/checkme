package com.example.krystianwsul.organizatortest.gui.tasks;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Schedule;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

public class SchedulePickerFragment extends Fragment implements HourMinutePickerFragment.HourMinutePickerFragmentListener, DatePickerFragment.DatePickerFragmentListener {
    private Spinner mSchedulePickerSpinner;

    private static final String POSITION_KEY = "position";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Assert.assertTrue(context instanceof DatePickerFragment.DatePickerFragmentListener);
        Assert.assertTrue(context instanceof HourMinutePickerFragment.HourMinutePickerFragmentListener);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        return layoutInflater.inflate(R.layout.fragment_schedule_picker, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        int count = 1;
        if (savedInstanceState != null) {
            int position = savedInstanceState.getInt(POSITION_KEY, -1);
            Assert.assertTrue(position != -1);
            if (position > 0)
                count = 2;
        }

        final int finalCount = count;

        if (savedInstanceState == null)
            loadFragment(0);

        View view = getView();
        Assert.assertTrue(view != null);

        mSchedulePickerSpinner = (Spinner) view.findViewById(R.id.schedule_picker_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.schedule_spinner, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSchedulePickerSpinner.setAdapter(adapter);

        mSchedulePickerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int mCount = finalCount;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Assert.assertTrue(position >= 0);
                Assert.assertTrue(position < 3);

                if (mCount > 0) {
                    mCount--;
                    return;
                }

                loadFragment(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void loadFragment(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < 3);

        Fragment fragment = createFragment(position);
        Assert.assertTrue(fragment != null);

        getChildFragmentManager().beginTransaction().replace(R.id.schedule_picker_frame, fragment).commit();
    }

    private Fragment createFragment(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < 3);

        switch (position) {
            case 0:
                return SingleScheduleFragment.newInstance();
            case 1:
                return DailyScheduleFragment.newInstance();
            case 2:
                return WeeklyScheduleFragment.newInstance();
            default:
                return null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(POSITION_KEY, mSchedulePickerSpinner.getSelectedItemPosition());
    }

    public Schedule createSchedule(RootTask rootTask) {
        Assert.assertTrue(rootTask != null);

        ScheduleFragment scheduleFragment = (ScheduleFragment) getChildFragmentManager().findFragmentById(R.id.schedule_picker_frame);
        Assert.assertTrue(scheduleFragment != null);

        return scheduleFragment.createSchedule(rootTask);
    }

    @Override
    public void onDatePickerFragmentResult(Date date) {
        Assert.assertTrue(date != null);

        DatePickerFragment.DatePickerFragmentListener datePickerFragmentListener = (DatePickerFragment.DatePickerFragmentListener) getChildFragmentManager().findFragmentById(R.id.schedule_picker_frame);
        Assert.assertTrue(datePickerFragmentListener != null);

        datePickerFragmentListener.onDatePickerFragmentResult(date);
    }

    @Override
    public void onHourMinutePickerFragmentResult(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);

        HourMinutePickerFragment.HourMinutePickerFragmentListener hourMinutePickerFragmentListener = (HourMinutePickerFragment.HourMinutePickerFragmentListener) getChildFragmentManager().findFragmentById(R.id.schedule_picker_frame);
        Assert.assertTrue(hourMinutePickerFragmentListener != null);

        hourMinutePickerFragmentListener.onHourMinutePickerFragmentResult(hourMinute);
    }
}
