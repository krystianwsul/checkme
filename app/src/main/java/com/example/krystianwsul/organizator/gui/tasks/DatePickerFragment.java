package com.example.krystianwsul.organizator.gui.tasks;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import com.example.krystianwsul.organizator.utils.time.Date;

import junit.framework.Assert;

public class DatePickerFragment extends DialogFragment {
    private final static String YEAR_KEY = "year";
    private final static String MONTH_KEY = "month";
    private final static String DAY_KEY = "day";

    public static DatePickerFragment newInstance(Activity activity, Date date) {
        Assert.assertTrue(activity != null);
        Assert.assertTrue(activity instanceof DatePickerFragmentListener);
        Assert.assertTrue(date != null);

        DatePickerFragment timePickerFragment = new DatePickerFragment();

        Bundle args = new Bundle();
        args.putInt(YEAR_KEY, date.getYear());
        args.putInt(MONTH_KEY, date.getMonth());
        args.putInt(DAY_KEY, date.getDay());
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

        int year = args.getInt(YEAR_KEY);
        int month = args.getInt(MONTH_KEY);
        int day = args.getInt(DAY_KEY);

        return new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                if (!view.isShown())
                    return;

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