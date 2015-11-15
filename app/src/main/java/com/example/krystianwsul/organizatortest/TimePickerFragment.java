package com.example.krystianwsul.organizatortest;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

import java.security.AccessControlContext;

/**
 * Created by Krystian on 11/13/2015.
 */
public class TimePickerFragment extends DialogFragment {
    public static TimePickerFragment newInstance(CustomTime customTime, DayOfWeek dayOfWeek) {
        Assert.assertTrue(customTime != null);
        Assert.assertTrue(dayOfWeek != null);

        TimePickerFragment timePickerFragment = new TimePickerFragment();

        Bundle args = new Bundle();
        args.putInt("customTimeId", customTime.getId());
        args.putSerializable("dayOfWeek", dayOfWeek);
        timePickerFragment.setArguments(args);

        return timePickerFragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Assert.assertTrue(activity instanceof TimePickerFragmentListener);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();

        int customTimeId = args.getInt("customTimeId");
        final CustomTime customTime = CustomTimeFactory.getInstance().getCustomTime(customTimeId);
        Assert.assertTrue(customTime != null);

        final DayOfWeek dayOfWeek = (DayOfWeek) args.getSerializable("dayOfWeek");
        Assert.assertTrue(dayOfWeek != null);

        HourMinute hourMinute = customTime.getHourMinute(dayOfWeek);
        Assert.assertTrue(hourMinute != null);

        return new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                HourMinute newHourMinute = new HourMinute(hourOfDay, minute);
                customTime.setHourMinute(dayOfWeek, newHourMinute);

                TimePickerFragmentListener timePickerFragmentListener = (TimePickerFragmentListener) getActivity();
                timePickerFragmentListener.onTimePickerFragmentResult(dayOfWeek);
            }
        }, hourMinute.getHour(), hourMinute.getMinute(), DateFormat.is24HourFormat(getActivity()));
    }

    public interface TimePickerFragmentListener {
        void onTimePickerFragmentResult(DayOfWeek dayOfWeek);
    }
}
