package com.example.krystianwsul.organizatortest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 11/11/2015.
 */
public class CustomTimesAdapter extends ArrayAdapter<CustomTime> {
    private final Context mContext;
    private final ArrayList<CustomTime> mCustomTimes;

    public CustomTimesAdapter(Context context) {
        super(context, -1, new ArrayList<>(CustomTime.getCustomTimes()));

        Assert.assertTrue(context != null);
        mContext = context;

        mCustomTimes = new ArrayList<>(CustomTime.getCustomTimes());
        Assert.assertTrue(mCustomTimes != null);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        CustomTime customTime = mCustomTimes.get(position);

        View showTimesRow = inflater.inflate(R.layout.show_custom_times_row, parent, false);

        TextView timesRowName = (TextView) showTimesRow.findViewById(R.id.times_row_name);
        timesRowName.setText(customTime.getName());

        return showTimesRow;
    }
}
