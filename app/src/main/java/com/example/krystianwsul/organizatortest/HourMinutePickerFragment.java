package com.example.krystianwsul.organizatortest;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

public class HourMinutePickerFragment extends DialogFragment {
    public static HourMinutePickerFragment newInstance(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);

        HourMinutePickerFragment hourMinutePickerFragment = new HourMinutePickerFragment();

        Bundle args = new Bundle();
        args.putInt("hour", hourMinute.getHour());
        args.putInt("minute", hourMinute.getMinute());
        hourMinutePickerFragment.setArguments(args);

        return hourMinutePickerFragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Assert.assertTrue(activity instanceof TimePickerFragmentListener);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();

        int hour = args.getInt("hour");
        int minute = args.getInt("minute");

        HourMinute hourMinute = new HourMinute(hour, minute);

        return new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                HourMinute newHourMinute = new HourMinute(hourOfDay, minute);

                TimePickerFragmentListener timePickerFragmentListener = (TimePickerFragmentListener) getActivity();
                timePickerFragmentListener.onTimePickerFragmentResult(newHourMinute);
            }
        }, hourMinute.getHour(), hourMinute.getMinute(), DateFormat.is24HourFormat(getActivity()));
    }

    public interface TimePickerFragmentListener {
        void onTimePickerFragmentResult(HourMinute hourMinute);
    }
}
