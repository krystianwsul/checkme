package com.example.krystianwsul.organizator.gui.tasks;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
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
import com.example.krystianwsul.organizator.domainmodel.CustomTime;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.NormalTime;
import com.example.krystianwsul.organizator.utils.time.Time;

import junit.framework.Assert;

import java.util.ArrayList;

public class TimePickerView extends LinearLayout {
    private Spinner mCustomTimeView;
    private TextView mHourMinuteView;

    private ArrayAdapter<SpinnerItem> mSpinnerAdapter;
    private OtherSpinnerItem mOtherSpinnerItem;

    private OnTimeSelectedListener mOnTimeSelectedListener;

    private static final String PARENT_KEY = "parent";
    private static final String CUSTOM_TIME_ID_KEY = "customTimeId";
    private static final String HOUR_KEY = "hour";
    private static final String MINUTE_KEY = "minute";

    private CustomTime mCustomTime;
    private HourMinute mHourMinute;

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

        //adapter

        ArrayList<SpinnerItem> spinnerTimes = new ArrayList<>();
        for (CustomTime customTime : DomainFactory.getInstance().getCustomTimeFactory().getCustomTimes())
            spinnerTimes.add(new TimeSpinnerItem(customTime));
        mOtherSpinnerItem = new OtherSpinnerItem(getContext());
        spinnerTimes.add(mOtherSpinnerItem);

        mSpinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, spinnerTimes);
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mCustomTimeView.setAdapter(mSpinnerAdapter);

        //initial values

        setSpinnerPosition(mSpinnerAdapter.getPosition(mOtherSpinnerItem));

        mHourMinute = HourMinute.getNow();
        mHourMinuteView.setVisibility(View.VISIBLE);
        mHourMinuteView.setText(mHourMinute.toString());

        //listeners

        mCustomTimeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Assert.assertTrue(position >= 0);
                Assert.assertTrue(position < mSpinnerAdapter.getCount());

                Assert.assertTrue(mOnTimeSelectedListener != null);

                SpinnerItem spinnerItem = mSpinnerAdapter.getItem(position);
                Assert.assertTrue(spinnerItem != null);

                if (mInternalSelection != null && mInternalSelection.equals(position)) {
                    mInternalSelection = null;
                    return;
                }

                if (spinnerItem == mOtherSpinnerItem) {
                    mHourMinute = HourMinute.getNow();
                    mCustomTime = null;

                    mHourMinuteView.setVisibility(View.VISIBLE);
                    mHourMinuteView.setText(mHourMinute.toString());

                    mOnTimeSelectedListener.onHourMinuteSelected(mHourMinute);
                } else {
                    TimeSpinnerItem timeSpinnerItem = (TimeSpinnerItem) spinnerItem;

                    mCustomTime = timeSpinnerItem.getCustomTime();
                    Assert.assertTrue(mCustomTime != null);

                    mHourMinute = null;

                    TimePickerView.this.mCustomTime = mCustomTime;
                    mHourMinuteView.setVisibility(View.INVISIBLE);

                    mOnTimeSelectedListener.onCustomTimeSelected(mCustomTime);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mHourMinuteView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Assert.assertTrue(mOnTimeSelectedListener != null);
                mOnTimeSelectedListener.onHourMinuteClick();
            }
        });
    }

    public void setOnTimeSelectedListener(OnTimeSelectedListener onTimeSelectedListener) {
        Assert.assertTrue(onTimeSelectedListener != null);
        mOnTimeSelectedListener = onTimeSelectedListener;
    }

    public void setHourMinute(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);

        mHourMinute = hourMinute;
        mCustomTime = null;

        mHourMinuteView.setVisibility(View.VISIBLE);
        mHourMinuteView.setText(hourMinute.toString());

        setSpinnerPosition(getOtherPosition());
    }

    public void setCustomTime(CustomTime customTime) {
        Assert.assertTrue(customTime != null);

        mCustomTime = customTime;
        mHourMinute = null;

        mHourMinuteView.setVisibility(View.INVISIBLE);

        setSpinnerPosition(getCustomTimePosition(customTime));
    }

    public void setTime(Time time) {
        Assert.assertTrue(time != null);

        if (time instanceof CustomTime) {
            setCustomTime((CustomTime) time);
        } else {
            Assert.assertTrue(time instanceof NormalTime);
            setHourMinute(((NormalTime) time).getHourMinute());
        }
    }

    public HourMinute getHourMinute() {
        return mHourMinute;
    }

    public CustomTime getCustomTime() {
        return mCustomTime;
    }

    public Time getTime() {
        if (mCustomTime != null) {
            Assert.assertTrue(mHourMinute == null);
            return mCustomTime;
        } else {
            Assert.assertTrue(mHourMinute != null);
            return new NormalTime(mHourMinute);
        }
    }

    private void setSpinnerPosition(int position) {
        mInternalSelection = position;
        mCustomTimeView.setSelection(position);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();

        bundle.putParcelable(PARENT_KEY, super.onSaveInstanceState());

        if (mCustomTime != null) {
            Assert.assertTrue(mHourMinute == null);
            bundle.putInt(CUSTOM_TIME_ID_KEY, mCustomTime.getId());
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

        if (customTimeId != -1) {
            mCustomTime = DomainFactory.getInstance().getCustomTimeFactory().getCustomTime(customTimeId);
            int position = getCustomTimePosition(mCustomTime);

            setSpinnerPosition(position);

            mHourMinute = null;
            mHourMinuteView.setVisibility(View.INVISIBLE);
        } else {
            int position = mSpinnerAdapter.getPosition(mOtherSpinnerItem);

            setSpinnerPosition(position);

            mHourMinute = new HourMinute(hour, minute);
            mHourMinuteView.setVisibility(View.VISIBLE);
            mHourMinuteView.setText(mHourMinute.toString());
        }
    }

    private int getCustomTimePosition(CustomTime customTime) {
        Assert.assertTrue(customTime != null);
        Assert.assertTrue(mSpinnerAdapter != null);

        for (int i = 0; i < mSpinnerAdapter.getCount(); i++) {
            SpinnerItem spinnerItem = mSpinnerAdapter.getItem(i);
            Assert.assertTrue(spinnerItem != mOtherSpinnerItem);

            TimeSpinnerItem timeSpinnerItem = (TimeSpinnerItem) spinnerItem;
            if (timeSpinnerItem.getCustomTime() == customTime)
                return i;
        }

        throw new IndexOutOfBoundsException("nie ma CustomTime (id == " + customTime.getId() + " w mSpinnerAdapter");
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
        private final CustomTime mCustomTime;

        public TimeSpinnerItem(CustomTime customTime) {
            Assert.assertTrue(customTime != null);
            mCustomTime = customTime;
        }

        public String toString() {
            return mCustomTime.toString();
        }

        public CustomTime getCustomTime() {
            return mCustomTime;
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
        public abstract void onCustomTimeSelected(CustomTime customTime);
        public abstract void onHourMinuteSelected(HourMinute hourMinute);
        public abstract void onHourMinuteClick();
    }
}