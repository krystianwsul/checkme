package com.example.krystianwsul.organizator.gui.tasks;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

public class TimePickerView extends LinearLayout {
    private HashMap<Integer, CustomTimeData> mCustomTimeDatas;

    private Spinner mCustomTimeView;
    private TextView mHourMinuteView;

    private ArrayAdapter<SpinnerItem> mSpinnerAdapter;
    private OtherSpinnerItem mOtherSpinnerItem;

    private OnTimeSelectedListener mOnTimeSelectedListener;

    private static final String PARENT_KEY = "parent";
    private static final String CUSTOM_TIME_ID_KEY = "customTimeId";
    private static final String HOUR_KEY = "hour";
    private static final String MINUTE_KEY = "minute";

    private Integer mCustomTimeId = null;
    private HourMinute mHourMinute = null;

    private Integer mInternalSelection = null;

    public TimePickerView(Context context) {
        super(context);
        initializeViews(context);
    }

    public TimePickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
    }

    public TimePickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeViews(context);
    }

    private void initializeViews(Context context) {
        Assert.assertTrue(context != null);

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        Assert.assertTrue(layoutInflater != null);

        layoutInflater.inflate(R.layout.view_time_picker, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mCustomTimeView = (Spinner) findViewById(R.id.time_picker_customtime);
        Assert.assertTrue(mCustomTimeView != null);

        mHourMinuteView = (TextView) findViewById(R.id.time_picker_hourminute);
        Assert.assertTrue(mHourMinuteView != null);

        //initial values

        mHourMinute = HourMinute.getNow();
        mHourMinuteView.setVisibility(View.VISIBLE);
        mHourMinuteView.setText(mHourMinute.toString());

        //listeners

        mCustomTimeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Assert.assertTrue(position >= 0);
                Assert.assertTrue(position < mSpinnerAdapter.getCount());

                SpinnerItem spinnerItem = mSpinnerAdapter.getItem(position);
                Assert.assertTrue(spinnerItem != null);

                if (mInternalSelection != null && mInternalSelection.equals(position)) {
                    mInternalSelection = null;
                    return;
                }

                if (spinnerItem == mOtherSpinnerItem) {
                    mHourMinute = HourMinute.getNow();
                    mCustomTimeId = null;

                    mHourMinuteView.setVisibility(View.VISIBLE);
                    mHourMinuteView.setText(mHourMinute.toString());

                    if (mOnTimeSelectedListener != null)
                        mOnTimeSelectedListener.onHourMinuteSelected(mHourMinute);
                } else {
                    TimeSpinnerItem timeSpinnerItem = (TimeSpinnerItem) spinnerItem;

                    mCustomTimeId = timeSpinnerItem.getCustomTimeId();

                    mHourMinute = null;

                    mHourMinuteView.setVisibility(View.INVISIBLE);

                    if (mOnTimeSelectedListener != null)
                        mOnTimeSelectedListener.onCustomTimeSelected(mCustomTimeId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mHourMinuteView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnTimeSelectedListener != null)
                    mOnTimeSelectedListener.onHourMinuteClick();
            }
        });
    }

    public void setCustomTimeDatas(HashMap<Integer, CustomTimeData> customTimeDatas) {
        Assert.assertTrue(customTimeDatas != null);
        mCustomTimeDatas = customTimeDatas;

        ArrayList<SpinnerItem> spinnerTimes = new ArrayList<>();
        for (CustomTimeData customTimeData : mCustomTimeDatas.values())
            spinnerTimes.add(new TimeSpinnerItem(customTimeData));
        mOtherSpinnerItem = new OtherSpinnerItem(getContext());
        spinnerTimes.add(mOtherSpinnerItem);

        mSpinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, spinnerTimes);
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mCustomTimeView.setAdapter(mSpinnerAdapter);

        setSpinnerPosition(mSpinnerAdapter.getPosition(mOtherSpinnerItem));

        if (mCustomTimeId != null)
            setCustomTimeId(mCustomTimeId);
        else if (mHourMinute != null)
            setHourMinute(mHourMinute);
    }

    public void setOnTimeSelectedListener(OnTimeSelectedListener onTimeSelectedListener) {
        mOnTimeSelectedListener = onTimeSelectedListener;
    }

    public void setHourMinute(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);

        mHourMinute = hourMinute;
        mCustomTimeId = null;

        mHourMinuteView.setVisibility(View.VISIBLE);
        mHourMinuteView.setText(hourMinute.toString());

        setSpinnerPosition(getOtherPosition());
    }

    public void setCustomTimeId(int customTimeId) {
        mCustomTimeId = customTimeId;
        mHourMinute = null;

        mHourMinuteView.setVisibility(View.INVISIBLE);

        setSpinnerPosition(getCustomTimeIdPosition(mCustomTimeId));
    }

    public HourMinute getHourMinute() {
        Assert.assertTrue(mCustomTimeDatas != null);
        return mHourMinute;
    }

    public Integer getCustomTimeId() {
        Assert.assertTrue(mCustomTimeDatas != null);
        return mCustomTimeId;
    }

    private void setSpinnerPosition(int position) {
        mInternalSelection = position;
        mCustomTimeView.setSelection(position);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();

        bundle.putParcelable(PARENT_KEY, super.onSaveInstanceState());

        if (mCustomTimeId != null) {
            Assert.assertTrue(mHourMinute == null);
            bundle.putInt(CUSTOM_TIME_ID_KEY, mCustomTimeId);
        } else {
            Assert.assertTrue(mHourMinute != null);
            bundle.putInt(HOUR_KEY, mHourMinute.getHour());
            bundle.putInt(MINUTE_KEY, mHourMinute.getMinute());
        }

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Assert.assertTrue(state != null);

        Bundle bundle = (Bundle) state;

        super.onRestoreInstanceState(bundle.getParcelable(PARENT_KEY));

        int customTimeId = bundle.getInt(CUSTOM_TIME_ID_KEY, -1);
        int hour = bundle.getInt(HOUR_KEY, -1);
        int minute = bundle.getInt(MINUTE_KEY, -1);

        Assert.assertTrue((hour == -1) == (minute == -1));
        Assert.assertTrue((customTimeId == -1) != (hour == -1));

        if (mCustomTimeDatas != null) {
            if (customTimeId != -1)
                setCustomTimeId(customTimeId);
            else
                setHourMinute(new HourMinute(hour, minute));
        }
    }

    private int getCustomTimeIdPosition(int customTimeId) {
        Assert.assertTrue(mSpinnerAdapter != null);

        for (int i = 0; i < mSpinnerAdapter.getCount(); i++) {
            SpinnerItem spinnerItem = mSpinnerAdapter.getItem(i);
            Assert.assertTrue(spinnerItem != mOtherSpinnerItem);

            TimeSpinnerItem timeSpinnerItem = (TimeSpinnerItem) spinnerItem;
            if (timeSpinnerItem.getCustomTimeId() == customTimeId)
                return i;
        }

        throw new IndexOutOfBoundsException("nie ma CustomTime (id == " + customTimeId + " w mSpinnerAdapter");
    }

    private int getOtherPosition() {
        Assert.assertTrue(mOtherSpinnerItem != null);
        Assert.assertTrue(mSpinnerAdapter != null);

        return mSpinnerAdapter.getPosition(mOtherSpinnerItem);
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        super.dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        super.dispatchThawSelfOnly(container);
    }

    private interface SpinnerItem {

    }

    private class TimeSpinnerItem implements SpinnerItem {
        private final CustomTimeData mCustomTimeData;

        public TimeSpinnerItem(CustomTimeData customTimeData) {
            Assert.assertTrue(customTimeData != null);
            mCustomTimeData = customTimeData;
        }

        public String toString() {
            return mCustomTimeData.Name;
        }

        public int getCustomTimeId() {
            return mCustomTimeData.Id;
        }
    }

    private class OtherSpinnerItem implements SpinnerItem {
        private final String mOther;

        public OtherSpinnerItem(Context context) {
            Assert.assertTrue(context != null);
            mOther = context.getString(R.string.other);
        }

        public String toString() {
            return mOther;
        }
    }

    public static abstract class OnTimeSelectedListener {
        public abstract void onCustomTimeSelected(int customTimeId);
        public abstract void onHourMinuteSelected(HourMinute hourMinute);
        public abstract void onHourMinuteClick();
    }

    public static class CustomTimeData {
        public final int Id;
        public final String Name;
        public final HashMap<DayOfWeek, HourMinute> HourMinutes;

        public CustomTimeData(int id, String name, HashMap<DayOfWeek, HourMinute> hourMinutes) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(hourMinutes != null);
            Assert.assertTrue(hourMinutes.size() == 7);

            Id = id;
            Name = name;
            HourMinutes = hourMinutes;
        }
    }
}