package com.example.krystianwsul.organizatortest;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;

import junit.framework.Assert;

public class DatePickerFragment extends DialogFragment {
    public static DatePickerFragment newInstance(Activity activity, Date date) {
        Assert.assertTrue(activity != null);
        Assert.assertTrue(activity instanceof DatePickerFragmentListener);
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
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();

        int year = args.getInt("year");
        int month = args.getInt("month");
        int day = args.getInt("day");

        Date date = new Date(year, month, day);

        return new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
            private boolean mFirst = true;

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                if (mFirst) {
                    mFirst = false;
                    return;
                }

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