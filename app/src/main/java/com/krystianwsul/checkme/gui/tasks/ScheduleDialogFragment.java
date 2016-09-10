package com.krystianwsul.checkme.gui.tasks;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDButton;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.MyCalendarFragment;
import com.krystianwsul.checkme.gui.TimeDialogFragment;
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity;
import com.krystianwsul.checkme.loaders.ScheduleLoader;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePairPersist;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Map;

public abstract class ScheduleDialogFragment extends DialogFragment {
    static final String SCHEDULE_DIALOG_DATA_KEY = "scheduleDialogData";

    private static final String DATE_FRAGMENT_TAG = "dateFragment";
    private static final String TIME_LIST_FRAGMENT_TAG = "timeListFragment";
    private static final String TIME_PICKER_TAG = "timePicker";

    private Spinner mScheduleType;

    TextInputLayout mScheduleDialogDateLayout;
    TextView mScheduleDialogDate;

    Spinner mScheduleDialogDay;

    TextInputLayout mScheduleDialogTimeLayout;
    TextView mScheduleDialogTime;

    MDButton mButton;

    Map<Integer, ScheduleLoader.CustomTimeData> mCustomTimeDatas;
    ScheduleDialogListener mScheduleDialogListener;

    Date mDate;
    DayOfWeek mDayOfWeek;
    TimePairPersist mTimePairPersist;

    private BroadcastReceiver mBroadcastReceiver;

    private final TimeDialogFragment.TimeDialogListener mTimeDialogListener = new TimeDialogFragment.TimeDialogListener() {
        @Override
        public void onCustomTimeSelected(int customTimeId) {
            Assert.assertTrue(mCustomTimeDatas != null);

            mTimePairPersist.setCustomTimeId(customTimeId);

            updateFields();
        }

        @Override
        public void onOtherSelected() {
            Assert.assertTrue(mCustomTimeDatas != null);

            RadialTimePickerDialogFragment radialTimePickerDialogFragment = new RadialTimePickerDialogFragment();
            radialTimePickerDialogFragment.setStartTime(mTimePairPersist.getHourMinute().getHour(), mTimePairPersist.getHourMinute().getMinute());
            radialTimePickerDialogFragment.setOnTimeSetListener(mOnTimeSetListener);
            radialTimePickerDialogFragment.show(getChildFragmentManager(), TIME_PICKER_TAG);
        }

        @Override
        public void onAddSelected() {
            startActivityForResult(ShowCustomTimeActivity.getCreateIntent(getActivity()), ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE);
        }
    };

    private final RadialTimePickerDialogFragment.OnTimeSetListener mOnTimeSetListener = (dialog, hourOfDay, minute) -> {
        Assert.assertTrue(mCustomTimeDatas != null);

        mTimePairPersist.setHourMinute(new HourMinute(hourOfDay, minute));
        updateFields();
    };

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog materialDialog = new MaterialDialog.Builder(getActivity())
                .customView(R.layout.fragment_schedule_dialog, false)
                .negativeText(android.R.string.cancel)
                .positiveText(android.R.string.ok)
                .onPositive((dialog, which) -> {
                    Assert.assertTrue(mCustomTimeDatas != null);
                    Assert.assertTrue(mScheduleDialogListener != null);
                    Assert.assertTrue(isValid());

                    mScheduleDialogListener.onScheduleDialogResult(new ScheduleDialogData(mDate, mTimePairPersist));
                })
                .build();

        View view = materialDialog.getCustomView();
        Assert.assertTrue(view != null);

        mScheduleType = (Spinner) view.findViewById(R.id.schedule_type);
        Assert.assertTrue(mScheduleType != null);

        mScheduleDialogDateLayout = (TextInputLayout) view.findViewById(R.id.schedule_dialog_date_layout);
        Assert.assertTrue(mScheduleDialogDateLayout != null);

        mScheduleDialogDate = (TextView) view.findViewById(R.id.schedule_dialog_date);
        Assert.assertTrue(mScheduleDialogDate != null);

        mScheduleDialogDay = (Spinner) view.findViewById(R.id.schedule_dialog_day);
        Assert.assertTrue(mScheduleDialogDay != null);

        mScheduleDialogTimeLayout = (TextInputLayout) view.findViewById(R.id.schedule_dialog_time_layout);
        Assert.assertTrue(mScheduleDialogTimeLayout != null);

        mScheduleDialogTime = (TextView) view.findViewById(R.id.schedule_dialog_time);
        Assert.assertTrue(mScheduleDialogTime != null);

        mButton = materialDialog.getActionButton(DialogAction.POSITIVE);
        Assert.assertTrue(mButton != null);

        return materialDialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ScheduleDialogData scheduleDialogData;
        if (savedInstanceState != null) {
            Assert.assertTrue(savedInstanceState.containsKey(SCHEDULE_DIALOG_DATA_KEY));

            scheduleDialogData = savedInstanceState.getParcelable(SCHEDULE_DIALOG_DATA_KEY);
        } else {
            Bundle args = getArguments();
            Assert.assertTrue(args != null);
            Assert.assertTrue(args.containsKey(SCHEDULE_DIALOG_DATA_KEY));

            scheduleDialogData = args.getParcelable(SCHEDULE_DIALOG_DATA_KEY);
        }

        Assert.assertTrue(scheduleDialogData != null);

        mDate = scheduleDialogData.mDate;
        mDayOfWeek = scheduleDialogData.mDayOfWeek;
        mTimePairPersist = scheduleDialogData.mTimePairPersist;

        switch (getScheduleType()) {
            case SINGLE:
                Assert.assertTrue(mDate != null);
                Assert.assertTrue(mTimePairPersist != null);

                break;
            case DAILY:
                Assert.assertTrue(mTimePairPersist != null);

                break;
            case WEEKLY:
                Assert.assertTrue(mDayOfWeek != null);
                Assert.assertTrue(mTimePairPersist != null);

                break;
            default:
                throw new UnsupportedOperationException();
        }

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.schedule_types, R.layout.spinner_no_padding);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mScheduleType.setAdapter(typeAdapter);

        mScheduleDialogTime.setOnClickListener(v -> {
            Assert.assertTrue(mCustomTimeDatas != null);

            ArrayList<TimeDialogFragment.CustomTimeData> customTimeDatas;

            switch (getScheduleType()) {
                case SINGLE:
                    Assert.assertTrue(mDate != null);

                    customTimeDatas = Stream.of(mCustomTimeDatas.values())
                            .sortBy(customTimeData -> customTimeData.HourMinutes.get(mDate.getDayOfWeek()))
                            .map(customTimeData -> new TimeDialogFragment.CustomTimeData(customTimeData.Id, customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDate.getDayOfWeek()) + ")"))
                            .collect(Collectors.toCollection(ArrayList::new));
                    break;
                case DAILY:
                    customTimeDatas = Stream.of(mCustomTimeDatas.values())
                            .sortBy(customTimeData -> customTimeData.Id)
                            .map(customTimeData -> new TimeDialogFragment.CustomTimeData(customTimeData.Id, customTimeData.Name))
                            .collect(Collectors.toCollection(ArrayList::new));
                    break;
                case WEEKLY:
                    Assert.assertTrue(mDayOfWeek != null);

                    customTimeDatas = Stream.of(mCustomTimeDatas.values())
                            .sortBy(customTimeData -> customTimeData.HourMinutes.get(mDayOfWeek))
                            .map(customTimeData -> new TimeDialogFragment.CustomTimeData(customTimeData.Id, customTimeData.Name + " (" + customTimeData.HourMinutes.get(mDayOfWeek) + ")"))
                            .collect(Collectors.toCollection(ArrayList::new));
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            TimeDialogFragment timeDialogFragment = TimeDialogFragment.newInstance(customTimeDatas);
            Assert.assertTrue(timeDialogFragment != null);

            timeDialogFragment.setTimeDialogListener(mTimeDialogListener);

            timeDialogFragment.show(getChildFragmentManager(), TIME_LIST_FRAGMENT_TAG);
        });

        TimeDialogFragment timeDialogFragment = (TimeDialogFragment) getChildFragmentManager().findFragmentByTag(TIME_LIST_FRAGMENT_TAG);
        if (timeDialogFragment != null)
            timeDialogFragment.setTimeDialogListener(mTimeDialogListener);

        RadialTimePickerDialogFragment radialTimePickerDialogFragment = (RadialTimePickerDialogFragment) getChildFragmentManager().findFragmentByTag(TIME_PICKER_TAG);
        if (radialTimePickerDialogFragment != null)
            radialTimePickerDialogFragment.setOnTimeSetListener(mOnTimeSetListener);

        final CalendarDatePickerDialogFragment.OnDateSetListener onDateSetListener = (dialog, year, monthOfYear, dayOfMonth) -> {
            Assert.assertTrue(getScheduleType() == ScheduleType.SINGLE);

            mDate = new Date(year, monthOfYear + 1, dayOfMonth);
            updateFields();
        };

        mScheduleDialogDate.setOnClickListener(v -> {
            Assert.assertTrue(getScheduleType() == ScheduleType.SINGLE);
            Assert.assertTrue(mDate != null);

            MyCalendarFragment calendarDatePickerDialogFragment = new MyCalendarFragment();
            calendarDatePickerDialogFragment.setDate(mDate);
            calendarDatePickerDialogFragment.setOnDateSetListener(onDateSetListener);
            calendarDatePickerDialogFragment.show(getChildFragmentManager(), DATE_FRAGMENT_TAG);
        });

        CalendarDatePickerDialogFragment calendarDatePickerDialogFragment = (CalendarDatePickerDialogFragment) getChildFragmentManager().findFragmentByTag(DATE_FRAGMENT_TAG);
        if (calendarDatePickerDialogFragment != null) {
            Assert.assertTrue(getScheduleType() == ScheduleType.SINGLE);

            calendarDatePickerDialogFragment.setOnDateSetListener(onDateSetListener);
        }

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mCustomTimeDatas != null)
                    updateFields();
            }
        };

        ArrayAdapter<DayOfWeek> dayOfWeekAdapter = new ArrayAdapter<>(getContext(), R.layout.spinner_no_padding, DayOfWeek.values());
        dayOfWeekAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mScheduleDialogDay.setAdapter(dayOfWeekAdapter);
        mScheduleDialogDay.setSelection(dayOfWeekAdapter.getPosition(mDayOfWeek));

        mScheduleDialogDay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean mFirst = true;
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mFirst) {
                    mFirst = false;
                    return;
                }

                Assert.assertTrue(getScheduleType() == ScheduleType.WEEKLY);

                DayOfWeek dayOfWeek = dayOfWeekAdapter.getItem(position);
                Assert.assertTrue(dayOfWeek != null);

                mDayOfWeek = dayOfWeek;

                updateFields();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if (mCustomTimeDatas != null)
            initialize();
    }

    @Override
    public void onResume() {
        super.onResume();

        MyCrashlytics.log("ScheduleDialogFragment.onResume");

        getActivity().registerReceiver(mBroadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));

        if (mCustomTimeDatas != null)
            updateFields();
    }

    public void initialize(@NonNull Map<Integer, ScheduleLoader.CustomTimeData> customTimeDatas, @NonNull ScheduleDialogListener scheduleDialogListener) {
        mCustomTimeDatas = customTimeDatas;
        mScheduleDialogListener = scheduleDialogListener;

        if (getActivity() != null)
            initialize();
    }

    protected abstract void initialize();

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        switch (getScheduleType()) {
            case SINGLE:
                Assert.assertTrue(mDate != null);
                Assert.assertTrue(mTimePairPersist != null);

                break;
            case DAILY:
                Assert.assertTrue(mTimePairPersist != null);

                break;
            case WEEKLY:
                Assert.assertTrue(mDayOfWeek != null);
                Assert.assertTrue(mTimePairPersist != null);

                break;
            default:
                throw new UnsupportedOperationException();
        }

        outState.putParcelable(SCHEDULE_DIALOG_DATA_KEY, new ScheduleDialogData(mDate, mDayOfWeek, mTimePairPersist));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Assert.assertTrue(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE);
        Assert.assertTrue(resultCode >= 0);
        Assert.assertTrue(data == null);

        if (resultCode > 1) {
            mCustomTimeDatas = null;
            mTimePairPersist.setCustomTimeId(resultCode);
        }
    }

    @NonNull
    protected abstract ScheduleType getScheduleType();

    protected abstract void updateFields();

    protected abstract boolean isValid();

    public static class ScheduleDialogData implements Parcelable {
        public final Date mDate;
        public final DayOfWeek mDayOfWeek;
        public final TimePairPersist mTimePairPersist;

        public ScheduleDialogData(@NonNull Date date, @NonNull TimePairPersist timePairPersist) {
            mDate = date;
            mDayOfWeek = null;
            mTimePairPersist = timePairPersist.copy();
        }

        public ScheduleDialogData(@NonNull TimePairPersist timePairPersist) {
            mDate = null;
            mDayOfWeek = null;
            mTimePairPersist = timePairPersist;
        }

        public ScheduleDialogData(@NonNull DayOfWeek dayOfWeek, @NonNull TimePairPersist timePairPersist) {
            mDate = null;
            mDayOfWeek = dayOfWeek;
            mTimePairPersist = timePairPersist.copy();
        }

        private ScheduleDialogData(Date date, DayOfWeek dayOfWeek, @NonNull TimePairPersist timePairPersist) {
            Assert.assertTrue((date == null) || (dayOfWeek == null));

            mDate = date;
            mDayOfWeek = dayOfWeek;
            mTimePairPersist = timePairPersist;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            Assert.assertTrue((mDate == null) || (mDayOfWeek == null));
            Assert.assertTrue(mTimePairPersist != null);

            dest.writeParcelable(mDate, flags);
            dest.writeSerializable(mDayOfWeek);
            dest.writeParcelable(mTimePairPersist, flags);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @SuppressWarnings("unused")
        public static final Creator<ScheduleDialogData> CREATOR = new Creator<ScheduleDialogData>() {
            @Override
            public ScheduleDialogData createFromParcel(Parcel in) {
                Date date = in.readParcelable(Date.class.getClassLoader());
                DayOfWeek dayOfWeek = (DayOfWeek) in.readSerializable();
                TimePairPersist timePairPersist = in.readParcelable(TimePairPersist.class.getClassLoader());

                Assert.assertTrue(timePairPersist != null);

                return new ScheduleDialogData(date, dayOfWeek, timePairPersist);
            }

            @Override
            public ScheduleDialogData[] newArray(int size) {
                return new ScheduleDialogData[size];
            }
        };
    }

    public interface ScheduleDialogListener {
        void onScheduleDialogResult(@NonNull ScheduleDialogData scheduleDialogData);
    }
}
