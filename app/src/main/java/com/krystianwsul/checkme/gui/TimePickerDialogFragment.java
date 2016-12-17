package com.krystianwsul.checkme.gui;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import com.krystianwsul.checkme.utils.time.HourMinute;

import junit.framework.Assert;

public class TimePickerDialogFragment extends AbstractDialogFragment {
    private static final String HOUR_MINUTE_KEY = "hourMinute";

    @Nullable
    private Listener mListener;

    private final TimePickerDialog.OnTimeSetListener mOnTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            Assert.assertTrue(mListener != null);

            mListener.onHourMinutePicked(new HourMinute(hourOfDay, minute));
        }
    };

    @NonNull
    public static TimePickerDialogFragment newInstance(@NonNull HourMinute hourMinute) {
        TimePickerDialogFragment timePickerDialogFragment = new TimePickerDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(HOUR_MINUTE_KEY, hourMinute);
        timePickerDialogFragment.setArguments(args);

        return timePickerDialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        Assert.assertTrue(args != null);
        Assert.assertTrue(args.containsKey(HOUR_MINUTE_KEY));

        HourMinute hourMinute = args.getParcelable(HOUR_MINUTE_KEY);
        Assert.assertTrue(hourMinute != null);

        return new TimePickerDialog(getActivity(), mOnTimeSetListener, hourMinute.getHour(), hourMinute.getMinute(), DateFormat.is24HourFormat(getActivity()));
    }

    public void setListener(@NonNull Listener listener) {
        Assert.assertTrue(mListener == null);

        mListener = listener;
    }

    public interface Listener {
        void onHourMinutePicked(@NonNull HourMinute hourMinute);
    }
}
