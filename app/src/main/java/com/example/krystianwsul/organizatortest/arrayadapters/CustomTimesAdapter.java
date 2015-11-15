package com.example.krystianwsul.organizatortest.arrayadapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 11/11/2015.
 */
public class CustomTimesAdapter extends ArrayAdapter<CustomTime> {
    private final Context mContext;
    private final ArrayList<CustomTime> mCustomTimes;

    public CustomTimesAdapter(Context context) {
        super(context, -1, new ArrayList<>(CustomTimeFactory.getInstance().getCustomTimes()));

        Assert.assertTrue(context != null);
        mContext = context;

        mCustomTimes = new ArrayList<>(CustomTimeFactory.getInstance().getCustomTimes());
        Assert.assertTrue(mCustomTimes != null);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.show_custom_times_row, parent, false);

            CustomTimeHolder customTimeHolder = new CustomTimeHolder();
            customTimeHolder.timesRowName = (TextView) convertView.findViewById(R.id.times_row_name);
            convertView.setTag(customTimeHolder);
        }

        CustomTimeHolder customTimeHolder = (CustomTimeHolder) convertView.getTag();

        CustomTime customTime = mCustomTimes.get(position);

        customTimeHolder.timesRowName.setText(customTime.getName());

        return convertView;
    }

    private class CustomTimeHolder {
        public TextView timesRowName;
    }
}
