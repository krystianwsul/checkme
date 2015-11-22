package com.example.krystianwsul.organizatortest;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

/**
 * Created by Krystian on 11/13/2015.
 */
public class DatePickerFragment extends DialogFragment {
    public static DatePickerFragment newInstance(Date date) {
        Assert.assertTrue(date != null);

        DatePickerFragment timePickerFragment = new DatePickerFragment();

        Bundle args = new Bundle();
        args.putInt("year", date.getYear());
        args.putInt("month", date.getMonth());
        args.putInt("day", date.getDay());
        timePickerFragment.setArguments(args);

        return timePickerFragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Assert.assertTrue(activity instanceof DatePickerFragmentListener);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();

        int year = args.getInt("year");
        int month = args.getInt("month");
        int day = args.getInt("day");

        Date date = new Date(year, month, day);

        return new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Date newDate = new Date(year, monthOfYear + 1, dayOfMonth);

                DatePickerFragmentListener datePickerFragmentListener = (DatePickerFragmentListener) getActivity();
                datePickerFragmentListener.onDatePickerFragmentResult(newDate);
            }
        }, year, month - 1, day);
    }

    public interface DatePickerFragmentListener {
        void onDatePickerFragmentResult(Date date);
    }
}