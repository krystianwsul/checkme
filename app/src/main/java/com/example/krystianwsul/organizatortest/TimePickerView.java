package com.example.krystianwsul.organizatortest;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import junit.framework.Assert;

public class TimePickerView extends LinearLayout {
    private Spinner mCustomTimeView;
    private TextView mHourMinuteView;

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
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        layoutInflater.inflate(R.layout.view_time_picker, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mCustomTimeView = (Spinner) findViewById(R.id.time_picker_customtime);
        Assert.assertTrue(mCustomTimeView != null);

        mHourMinuteView = (TextView) findViewById(R.id.time_picker_hourminute);
        Assert.assertTrue(mHourMinuteView != null);
    }


    public void setAdapter(ArrayAdapter<?> arrayAdapter) {
        Assert.assertTrue(arrayAdapter != null);
        mCustomTimeView.setAdapter(arrayAdapter);
    }

    public void setSelection(int position) {
        mCustomTimeView.setSelection(position);
    }

    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener onItemSelectedListener) {
        mCustomTimeView.setOnItemSelectedListener(onItemSelectedListener);
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        mHourMinuteView.setOnClickListener(onClickListener);
    }

    public int getSelectedItemPosition() {
        return mCustomTimeView.getSelectedItemPosition();
    }

    public void setHourMinuteVisibility(int visibility) {
        mHourMinuteView.setVisibility(visibility);
    }

    public void setText(String text) {
        mHourMinuteView.setText(text);
    }
}