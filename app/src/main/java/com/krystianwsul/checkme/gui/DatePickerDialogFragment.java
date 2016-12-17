package com.krystianwsul.checkme.gui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

public class DatePickerDialogFragment extends AbstractDialogFragment {
    private static final String DATE_KEY = "date";

    @Nullable
    private Listener mListener;

    private final DatePickerDialog.OnDateSetListener mOnDateSetListener = (view, year, month, dayOfMonth) -> {
        Assert.assertTrue(mListener != null);

        mListener.onDatePicked(new Date(year, month + 1, dayOfMonth));
    };

    public static DatePickerDialogFragment newInstance(@NonNull Date date) {
        DatePickerDialogFragment datePickerDialogFragment = new DatePickerDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(DATE_KEY, date);
        datePickerDialogFragment.setArguments(args);

        return datePickerDialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        Assert.assertTrue(args != null);
        Assert.assertTrue(args.containsKey(DATE_KEY));

        Date date = args.getParcelable(DATE_KEY);
        Assert.assertTrue(date != null);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), mOnDateSetListener, date.getYear(), date.getMonth() - 1, date.getDay());
        datePickerDialog.getDatePicker().setMinDate(ExactTimeStamp.getNow().getLong());

        return datePickerDialog;
    }

    public void setListener(@NonNull Listener listener) {
        Assert.assertTrue(mListener == null);

        mListener = listener;
    }

    public interface Listener {
        void onDatePicked(@NonNull Date date);
    }
}
