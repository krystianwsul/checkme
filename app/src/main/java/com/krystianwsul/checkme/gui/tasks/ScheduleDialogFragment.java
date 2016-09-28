package com.krystianwsul.checkme.gui.tasks;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDButton;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.numberpicker.NumberPickerBuilder;
import com.codetroopers.betterpickers.numberpicker.NumberPickerDialogFragment;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.MyCalendarFragment;
import com.krystianwsul.checkme.gui.TimeDialogFragment;
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePairPersist;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Vector;

public class ScheduleDialogFragment extends DialogFragment {
    private static final String SCHEDULE_DIALOG_DATA_KEY = "scheduleDialogData";
    private static final String SHOW_DELETE_KEY = "showDelete";

    private static final String DATE_FRAGMENT_TAG = "dateFragment";
    private static final String TIME_LIST_FRAGMENT_TAG = "timeListFragment";
    private static final String TIME_PICKER_TAG = "timePicker";

    private static final String DAY_NUMBER_PICKER_TAG = "day_number_dialog";

    private Spinner mScheduleType;

    private TextInputLayout mScheduleDialogDateLayout;
    private TextView mScheduleDialogDate;

    private Spinner mScheduleDialogDay;

    private LinearLayout mScheduleDialogMonthLayout;

    private RadioButton mScheduleDialogMonthDayRadio;
    private TextView mScheduleDialogMonthDayNumber;
    private TextView mScheduleDialogMonthDayLabel;

    private RadioButton mScheduleDialogMonthWeekRadio;
    private Spinner mScheduleDialogMonthWeekNumber;
    private Spinner mScheduleDialogMonthWeekDay;

    private Spinner mScheduleDialogMonthEnd;

    private View mScheduleDialogDailyPadding;

    private TextInputLayout mScheduleDialogTimeLayout;
    private TextView mScheduleDialogTime;

    private MDButton mButton;

    private Map<Integer, CreateTaskLoader.CustomTimeData> mCustomTimeDatas;
    private ScheduleDialogListener mScheduleDialogListener;

    private ScheduleDialogData mScheduleDialogData;

    private BroadcastReceiver mBroadcastReceiver;

    private final TimeDialogFragment.TimeDialogListener mTimeDialogListener = new TimeDialogFragment.TimeDialogListener() {
        @Override
        public void onCustomTimeSelected(int customTimeId) {
            Assert.assertTrue(mCustomTimeDatas != null);

            mScheduleDialogData.mTimePairPersist.setCustomTimeId(customTimeId);

            updateFields();
        }

        @Override
        public void onOtherSelected() {
            Assert.assertTrue(mCustomTimeDatas != null);

            RadialTimePickerDialogFragment radialTimePickerDialogFragment = new RadialTimePickerDialogFragment();
            radialTimePickerDialogFragment.setStartTime(mScheduleDialogData.mTimePairPersist.getHourMinute().getHour(), mScheduleDialogData.mTimePairPersist.getHourMinute().getMinute());
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

        mScheduleDialogData.mTimePairPersist.setHourMinute(new HourMinute(hourOfDay, minute));
        updateFields();
    };

    private final NumberPickerDialogFragment.NumberPickerDialogHandlerV2 mDayNumberPickerDialogHandlerV2 = new NumberPickerDialogFragment.NumberPickerDialogHandlerV2() {
        @Override
        public void onDialogNumberSet(int reference, BigInteger number, double decimal, boolean isNegative, BigDecimal fullNumber) {
            mScheduleDialogData.mMonthDayNumber = fullNumber.intValue();
            Assert.assertTrue(mScheduleDialogData.mMonthDayNumber > 0);
            Assert.assertTrue(mScheduleDialogData.mMonthDayNumber < 29);

            updateFields();
        }
    };

    @NonNull
    public static ScheduleDialogFragment newInstance(@NonNull ScheduleDialogData scheduleDialogData, boolean showDelete) {
        ScheduleDialogFragment scheduleDialogFragment = new ScheduleDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(SCHEDULE_DIALOG_DATA_KEY, scheduleDialogData);
        args.putBoolean(SHOW_DELETE_KEY, showDelete);
        scheduleDialogFragment.setArguments(args);

        return scheduleDialogFragment;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        Assert.assertTrue(args != null);
        Assert.assertTrue(args.containsKey(SHOW_DELETE_KEY));

        boolean showDelete = args.getBoolean(SHOW_DELETE_KEY);

        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity())
                .customView(R.layout.fragment_schedule_dialog, false)
                .negativeText(android.R.string.cancel)
                .positiveText(android.R.string.ok)
                .onNegative(((dialog, which) -> dialog.cancel()))
                .onPositive((dialog, which) -> {
                    Assert.assertTrue(mCustomTimeDatas != null);
                    Assert.assertTrue(mScheduleDialogListener != null);
                    Assert.assertTrue(isValid());

                    mScheduleDialogListener.onScheduleDialogResult(mScheduleDialogData);
                });

        if (showDelete)
            builder.neutralText(R.string.delete)
                    .onNeutral((dialog, which) -> mScheduleDialogListener.onScheduleDialogDelete());

        MaterialDialog materialDialog = builder.build();

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

        mScheduleDialogMonthLayout = (LinearLayout) view.findViewById(R.id.schedule_dialog_month_layout);
        Assert.assertTrue(mScheduleDialogMonthLayout != null);

        mScheduleDialogMonthDayRadio = (RadioButton) view.findViewById(R.id.schedule_dialog_month_day_radio);
        Assert.assertTrue(mScheduleDialogMonthDayRadio != null);

        mScheduleDialogMonthDayNumber = (TextView) view.findViewById(R.id.schedule_dialog_month_day_number);
        Assert.assertTrue(mScheduleDialogMonthDayNumber != null);

        mScheduleDialogMonthDayLabel = (TextView) view.findViewById(R.id.schedule_dialog_month_day_label);
        Assert.assertTrue(mScheduleDialogMonthDayLabel != null);

        mScheduleDialogMonthWeekRadio = (RadioButton) view.findViewById(R.id.schedule_dialog_month_week_radio);
        Assert.assertTrue(mScheduleDialogMonthWeekRadio != null);

        mScheduleDialogMonthWeekNumber = (Spinner) view.findViewById(R.id.schedule_dialog_month_week_number);
        Assert.assertTrue(mScheduleDialogMonthWeekNumber != null);

        mScheduleDialogMonthWeekDay = (Spinner) view.findViewById(R.id.schedule_dialog_month_week_day);
        Assert.assertTrue(mScheduleDialogMonthWeekDay != null);

        mScheduleDialogMonthEnd = (Spinner) view.findViewById(R.id.schedule_dialog_month_end);
        Assert.assertTrue(mScheduleDialogMonthEnd != null);

        mScheduleDialogDailyPadding = view.findViewById(R.id.schedule_dialog_daily_padding);
        Assert.assertTrue(mScheduleDialogDailyPadding != null);

        mScheduleDialogTimeLayout = (TextInputLayout) view.findViewById(R.id.schedule_dialog_time_layout);
        Assert.assertTrue(mScheduleDialogTimeLayout != null);

        mScheduleDialogTime = (TextView) view.findViewById(R.id.schedule_dialog_time);
        Assert.assertTrue(mScheduleDialogTime != null);

        mButton = materialDialog.getActionButton(DialogAction.POSITIVE);
        Assert.assertTrue(mButton != null);

        return materialDialog;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            Assert.assertTrue(savedInstanceState.containsKey(SCHEDULE_DIALOG_DATA_KEY));

            mScheduleDialogData = savedInstanceState.getParcelable(SCHEDULE_DIALOG_DATA_KEY);
        } else {
            Bundle args = getArguments();
            Assert.assertTrue(args != null);
            Assert.assertTrue(args.containsKey(SCHEDULE_DIALOG_DATA_KEY));

            mScheduleDialogData = args.getParcelable(SCHEDULE_DIALOG_DATA_KEY);
        }

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.schedule_types, R.layout.spinner_no_padding);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mScheduleType.setAdapter(typeAdapter);

        mScheduleType.setSelection(mScheduleDialogData.mScheduleType.ordinal());

        mScheduleType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mScheduleDialogData.mScheduleType = ScheduleType.values()[i];

                if (getActivity() != null && mCustomTimeDatas != null)
                    initialize();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mScheduleDialogTime.setOnClickListener(v -> {
            Assert.assertTrue(mCustomTimeDatas != null);

            ArrayList<TimeDialogFragment.CustomTimeData> customTimeDatas;

            switch (mScheduleDialogData.mScheduleType) {
                case SINGLE:
                    customTimeDatas = Stream.of(mCustomTimeDatas.values())
                            .sortBy(customTimeData -> customTimeData.HourMinutes.get(mScheduleDialogData.mDate.getDayOfWeek()))
                            .map(customTimeData -> new TimeDialogFragment.CustomTimeData(customTimeData.Id, customTimeData.Name + " (" + customTimeData.HourMinutes.get(mScheduleDialogData.mDate.getDayOfWeek()) + ")"))
                            .collect(Collectors.toCollection(ArrayList::new));
                    break;
                case DAILY:
                    customTimeDatas = Stream.of(mCustomTimeDatas.values())
                            .sortBy(customTimeData -> customTimeData.Id)
                            .map(customTimeData -> new TimeDialogFragment.CustomTimeData(customTimeData.Id, customTimeData.Name))
                            .collect(Collectors.toCollection(ArrayList::new));
                    break;
                case WEEKLY:
                    customTimeDatas = Stream.of(mCustomTimeDatas.values())
                            .sortBy(customTimeData -> customTimeData.HourMinutes.get(mScheduleDialogData.mDayOfWeek))
                            .map(customTimeData -> new TimeDialogFragment.CustomTimeData(customTimeData.Id, customTimeData.Name + " (" + customTimeData.HourMinutes.get(mScheduleDialogData.mDayOfWeek) + ")"))
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
            Assert.assertTrue(mScheduleDialogData.mScheduleType == ScheduleType.SINGLE);

            mScheduleDialogData.mDate = new Date(year, monthOfYear + 1, dayOfMonth);
            updateFields();
        };

        mScheduleDialogDate.setOnClickListener(v -> {
            Assert.assertTrue(mScheduleDialogData.mScheduleType == ScheduleType.SINGLE);

            MyCalendarFragment calendarDatePickerDialogFragment = new MyCalendarFragment();
            calendarDatePickerDialogFragment.setDate(mScheduleDialogData.mDate);
            calendarDatePickerDialogFragment.setOnDateSetListener(onDateSetListener);
            calendarDatePickerDialogFragment.show(getChildFragmentManager(), DATE_FRAGMENT_TAG);
        });

        CalendarDatePickerDialogFragment calendarDatePickerDialogFragment = (CalendarDatePickerDialogFragment) getChildFragmentManager().findFragmentByTag(DATE_FRAGMENT_TAG);
        if (calendarDatePickerDialogFragment != null) {
            Assert.assertTrue(mScheduleDialogData.mScheduleType == ScheduleType.SINGLE);

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
        mScheduleDialogDay.setSelection(dayOfWeekAdapter.getPosition(mScheduleDialogData.mDayOfWeek));
        mScheduleDialogDay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                DayOfWeek dayOfWeek = dayOfWeekAdapter.getItem(position);
                Assert.assertTrue(dayOfWeek != null);

                mScheduleDialogData.mDayOfWeek = dayOfWeek;

                updateFields();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        int textPrimary = ContextCompat.getColor(getActivity(), R.color.textPrimary);
        int textDisabledSpinner = ContextCompat.getColor(getActivity(), R.color.textDisabledSpinner);

        mScheduleDialogMonthDayRadio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked)
                return;

            mScheduleDialogMonthWeekRadio.setChecked(false);

            mScheduleDialogData.mMonthDay = true;

            mScheduleDialogMonthDayNumber.setEnabled(true);
            mScheduleDialogMonthDayLabel.setTextColor(textPrimary);

            mScheduleDialogMonthWeekNumber.setEnabled(false);
            mScheduleDialogMonthWeekDay.setEnabled(false);
        });

        mScheduleDialogMonthDayRadio.setChecked(mScheduleDialogData.mMonthDay);

        mScheduleDialogMonthDayNumber.setOnClickListener(v -> new NumberPickerBuilder()
                .setPlusMinusVisibility(View.GONE)
                .setDecimalVisibility(View.GONE)
                .setMinNumber(BigDecimal.ONE)
                .setMaxNumber(new BigDecimal(28))
                .setStyleResId(R.style.BetterPickersDialogFragment_Light)
                .setFragmentManager(getChildFragmentManager())
                .addNumberPickerDialogHandler(mDayNumberPickerDialogHandlerV2)
                .show());

        NumberPickerDialogFragment dayNumberPickerDialogFragment = (NumberPickerDialogFragment) getChildFragmentManager().findFragmentByTag(DAY_NUMBER_PICKER_TAG);
        if (dayNumberPickerDialogFragment != null)
            dayNumberPickerDialogFragment.setNumberPickerDialogHandlersV2(new Vector<>(Collections.singletonList(mDayNumberPickerDialogHandlerV2)));

        mScheduleDialogMonthWeekRadio.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (!isChecked)
                return;

            mScheduleDialogMonthDayRadio.setChecked(false);

            mScheduleDialogData.mMonthDay = false;

            mScheduleDialogMonthDayNumber.setEnabled(false);
            mScheduleDialogMonthDayLabel.setTextColor(textDisabledSpinner);

            mScheduleDialogMonthWeekNumber.setEnabled(true);
            mScheduleDialogMonthWeekDay.setEnabled(true);
        });

        mScheduleDialogMonthWeekRadio.setChecked(!mScheduleDialogData.mMonthDay);

        ArrayAdapter<Integer> monthWeekNumberAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_no_padding, Arrays.asList(1, 2, 3, 4));
        monthWeekNumberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mScheduleDialogMonthWeekNumber.setAdapter(monthWeekNumberAdapter);
        mScheduleDialogMonthWeekNumber.setSelection(mScheduleDialogData.mMonthWeekNumber - 1);
        mScheduleDialogMonthWeekNumber.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Assert.assertTrue(position >= 0);
                Assert.assertTrue(position <= 3);

                mScheduleDialogData.mMonthWeekNumber = position + 1;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayAdapter<DayOfWeek> monthWeekDayAdapter = new ArrayAdapter<>(getContext(), R.layout.spinner_no_padding, DayOfWeek.values());
        monthWeekDayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mScheduleDialogMonthWeekDay.setAdapter(monthWeekDayAdapter);
        mScheduleDialogMonthWeekDay.setSelection(monthWeekDayAdapter.getPosition(mScheduleDialogData.mMonthWeekDay));
        mScheduleDialogMonthWeekDay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                DayOfWeek dayOfWeek = monthWeekDayAdapter.getItem(position);
                Assert.assertTrue(dayOfWeek != null);

                mScheduleDialogData.mMonthWeekDay = dayOfWeek;

                updateFields();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayAdapter<CharSequence> monthEndAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.month, R.layout.spinner_no_padding);
        monthEndAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mScheduleDialogMonthEnd.setAdapter(monthEndAdapter);

        mScheduleDialogMonthEnd.setSelection(mScheduleDialogData.mBeginningOfMonth ? 0 : 1);

        mScheduleDialogMonthEnd.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Assert.assertTrue(position == 0 || position == 1);

                mScheduleDialogData.mBeginningOfMonth = (position == 0);
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

    public void initialize(@NonNull Map<Integer, CreateTaskLoader.CustomTimeData> customTimeDatas, @NonNull ScheduleDialogListener scheduleDialogListener) {
        mCustomTimeDatas = customTimeDatas;
        mScheduleDialogListener = scheduleDialogListener;

        if (getActivity() != null)
            initialize();
    }

    private void initialize() {
        Assert.assertTrue(mCustomTimeDatas != null);
        Assert.assertTrue(mScheduleDialogListener != null);
        Assert.assertTrue(mScheduleDialogData != null);
        Assert.assertTrue(mScheduleDialogTime != null);
        Assert.assertTrue(getActivity() != null);

        switch (mScheduleDialogData.mScheduleType) {
            case SINGLE:
                mScheduleDialogDateLayout.setVisibility(View.VISIBLE);
                mScheduleDialogDay.setVisibility(View.GONE);
                mScheduleDialogMonthLayout.setVisibility(View.GONE);
                mScheduleDialogDailyPadding.setVisibility(View.GONE);
                mScheduleDialogTimeLayout.setVisibility(View.VISIBLE);
                mScheduleDialogTimeLayout.setErrorEnabled(true);
                break;
            case DAILY:
                mScheduleDialogDateLayout.setVisibility(View.GONE);
                mScheduleDialogDay.setVisibility(View.GONE);
                mScheduleDialogMonthLayout.setVisibility(View.GONE);
                mScheduleDialogDailyPadding.setVisibility(View.VISIBLE);
                mScheduleDialogTimeLayout.setVisibility(View.VISIBLE);
                mScheduleDialogTimeLayout.setErrorEnabled(false);
                break;
            case WEEKLY:
                mScheduleDialogDateLayout.setVisibility(View.GONE);
                mScheduleDialogDay.setVisibility(View.VISIBLE);
                mScheduleDialogMonthLayout.setVisibility(View.GONE);
                mScheduleDialogDailyPadding.setVisibility(View.VISIBLE);
                mScheduleDialogTimeLayout.setVisibility(View.VISIBLE);
                mScheduleDialogTimeLayout.setErrorEnabled(false);
                break;
            case MONTHLY:
                mScheduleDialogDateLayout.setVisibility(View.GONE);
                mScheduleDialogDay.setVisibility(View.GONE);
                mScheduleDialogMonthLayout.setVisibility(View.VISIBLE);
                mScheduleDialogDailyPadding.setVisibility(View.GONE);
                mScheduleDialogTimeLayout.setVisibility(View.VISIBLE);
                mScheduleDialogTimeLayout.setErrorEnabled(false);
                break;
            default:
                throw new UnsupportedOperationException();
        }

        updateFields();
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(SCHEDULE_DIALOG_DATA_KEY, mScheduleDialogData);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Assert.assertTrue(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE);
        Assert.assertTrue(resultCode >= 0);
        Assert.assertTrue(data == null);

        if (resultCode > 1) {
            mCustomTimeDatas = null;
            mScheduleDialogData.mTimePairPersist.setCustomTimeId(resultCode);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateFields() {
        Assert.assertTrue(mScheduleDialogData != null);
        Assert.assertTrue(mScheduleDialogDate != null);
        Assert.assertTrue(mScheduleDialogTime != null);
        Assert.assertTrue(mCustomTimeDatas != null);

        switch (mScheduleDialogData.mScheduleType) {
            case SINGLE:
                mScheduleDialogDate.setText(mScheduleDialogData.mDate.getDisplayText(getContext()));

                if (mScheduleDialogData.mTimePairPersist.getCustomTimeId() != null) {
                    CreateTaskLoader.CustomTimeData customTimeData = mCustomTimeDatas.get(mScheduleDialogData.mTimePairPersist.getCustomTimeId());
                    Assert.assertTrue(customTimeData != null);

                    mScheduleDialogTime.setText(customTimeData.Name + " (" + customTimeData.HourMinutes.get(mScheduleDialogData.mDate.getDayOfWeek()) + ")");
                } else {
                    mScheduleDialogTime.setText(mScheduleDialogData.mTimePairPersist.getHourMinute().toString());
                }

                break;
            case DAILY:
                if (mScheduleDialogData.mTimePairPersist.getCustomTimeId() != null) {
                    CreateTaskLoader.CustomTimeData customTimeData = mCustomTimeDatas.get(mScheduleDialogData.mTimePairPersist.getCustomTimeId());
                    Assert.assertTrue(customTimeData != null);

                    mScheduleDialogTime.setText(customTimeData.Name);
                } else {
                    mScheduleDialogTime.setText(mScheduleDialogData.mTimePairPersist.getHourMinute().toString());
                }

                break;
            case WEEKLY:
                if (mScheduleDialogData.mTimePairPersist.getCustomTimeId() != null) {
                    CreateTaskLoader.CustomTimeData customTimeData = mCustomTimeDatas.get(mScheduleDialogData.mTimePairPersist.getCustomTimeId());
                    Assert.assertTrue(customTimeData != null);

                    mScheduleDialogTime.setText(customTimeData.Name + " (" + customTimeData.HourMinutes.get(mScheduleDialogData.mDayOfWeek) + ")");
                } else {
                    mScheduleDialogTime.setText(mScheduleDialogData.mTimePairPersist.getHourMinute().toString());
                }

                break;
            case MONTHLY:
                mScheduleDialogMonthDayNumber.setText(Integer.valueOf(mScheduleDialogData.mMonthDayNumber).toString());

                if (mScheduleDialogData.mTimePairPersist.getCustomTimeId() != null) {
                    CreateTaskLoader.CustomTimeData customTimeData = mCustomTimeDatas.get(mScheduleDialogData.mTimePairPersist.getCustomTimeId());
                    Assert.assertTrue(customTimeData != null);

                    mScheduleDialogTime.setText(customTimeData.Name);
                } else {
                    mScheduleDialogTime.setText(mScheduleDialogData.mTimePairPersist.getHourMinute().toString());
                }

                break;
            default:
                throw new UnsupportedOperationException();
        }

        if (isValid()) {
            mButton.setEnabled(true);

            mScheduleDialogDateLayout.setError(null);
            mScheduleDialogTimeLayout.setError(null);
        } else {
            Assert.assertTrue(mScheduleDialogData.mScheduleType == ScheduleType.SINGLE);
            mButton.setEnabled(false);

            if (mScheduleDialogData.mDate.compareTo(Date.today()) >= 0) {
                mScheduleDialogDateLayout.setError(null);
                mScheduleDialogTimeLayout.setError(getString(R.string.error_time));
            } else {
                mScheduleDialogDateLayout.setError(getString(R.string.error_date));
                mScheduleDialogTimeLayout.setError(null);
            }
        }
    }

    private boolean isValid() {
        if (mCustomTimeDatas == null)
            return false;

        if (mScheduleDialogData == null)
            return false;

        if (mScheduleDialogData.mScheduleType != ScheduleType.SINGLE)
            return true;

        HourMinute hourMinute;
        if (mScheduleDialogData.mTimePairPersist.getCustomTimeId() != null) {
            if (!mCustomTimeDatas.containsKey(mScheduleDialogData.mTimePairPersist.getCustomTimeId()))
                return false; //cached data doesn't contain new custom time

            hourMinute = mCustomTimeDatas.get(mScheduleDialogData.mTimePairPersist.getCustomTimeId()).HourMinutes.get(mScheduleDialogData.mDate.getDayOfWeek());
        } else {
            hourMinute = mScheduleDialogData.mTimePairPersist.getHourMinute();
        }

        return (new TimeStamp(mScheduleDialogData.mDate, hourMinute).compareTo(TimeStamp.getNow()) > 0);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        Assert.assertTrue(mScheduleDialogListener != null);

        mScheduleDialogListener.onScheduleDialogCancel();
    }

    public static class ScheduleDialogData implements Parcelable {
        Date mDate;
        DayOfWeek mDayOfWeek;
        boolean mMonthDay;
        int mMonthDayNumber;
        int mMonthWeekNumber;
        DayOfWeek mMonthWeekDay;
        boolean mBeginningOfMonth;
        final TimePairPersist mTimePairPersist;
        ScheduleType mScheduleType;

        ScheduleDialogData(@NonNull Date date, @NonNull DayOfWeek dayOfWeek, boolean monthDay, int monthDayNumber, int monthWeekNumber, @NonNull DayOfWeek monthWeekDay, boolean beginningOfMonth, @NonNull TimePairPersist timePairPersist, @NonNull ScheduleType scheduleType) {
            Assert.assertTrue(monthDayNumber > 0);
            Assert.assertTrue(monthDayNumber < 29);
            Assert.assertTrue(monthWeekNumber > 0);
            Assert.assertTrue(monthWeekNumber < 5);

            mDate = date;
            mDayOfWeek = dayOfWeek;
            mMonthDay = monthDay;
            mMonthDayNumber = monthDayNumber;
            mMonthWeekNumber = monthWeekNumber;
            mMonthWeekDay = monthWeekDay;
            mBeginningOfMonth = beginningOfMonth;
            mTimePairPersist = timePairPersist;
            mScheduleType = scheduleType;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(mDate, flags);
            dest.writeSerializable(mDayOfWeek);
            dest.writeInt(mMonthDay ? 1 : 0);
            dest.writeInt(mMonthDayNumber);
            dest.writeInt(mMonthWeekNumber);
            dest.writeSerializable(mMonthWeekDay);
            dest.writeInt(mBeginningOfMonth ? 1 : 0);
            dest.writeParcelable(mTimePairPersist, flags);
            dest.writeSerializable(mScheduleType);
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
                boolean monthDay = (in.readInt() == 1);
                int monthDayNumber = in.readInt();
                int monthWeekNumber = in.readInt();
                DayOfWeek monthWeekDay = (DayOfWeek) in.readSerializable();
                boolean beginningOfMonth = (in.readInt() == 1);
                TimePairPersist timePairPersist = in.readParcelable(TimePairPersist.class.getClassLoader());
                ScheduleType scheduleType = (ScheduleType) in.readSerializable();

                return new ScheduleDialogData(date, dayOfWeek, monthDay, monthDayNumber, monthWeekNumber, monthWeekDay, beginningOfMonth, timePairPersist, scheduleType);
            }

            @Override
            public ScheduleDialogData[] newArray(int size) {
                return new ScheduleDialogData[size];
            }
        };
    }

    public interface ScheduleDialogListener {
        void onScheduleDialogResult(@NonNull ScheduleDialogData scheduleDialogData);

        void onScheduleDialogDelete();

        void onScheduleDialogCancel();
    }
}
