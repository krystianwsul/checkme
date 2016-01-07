package com.example.krystianwsul.organizator.gui.customtimes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.CustomTime;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowCustomTimesActivity extends AppCompatActivity {
    private ListView mShowTimesList;

    public static Intent getIntent(Context context) {
        return new Intent(context, ShowCustomTimesActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_custom_times);

        mShowTimesList = (ListView) findViewById(R.id.show_times_list);
        mShowTimesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CustomTime customTime = (CustomTime) parent.getItemAtPosition(position);
                startActivity(ShowCustomTimeActivity.getEditIntent(customTime, ShowCustomTimesActivity.this));
            }
        });

        FloatingActionButton showTimesFab = (FloatingActionButton) findViewById(R.id.show_times_fab);
        showTimesFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(ShowCustomTimeActivity.getCreateIntent(ShowCustomTimesActivity.this));
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        mShowTimesList.setAdapter(new CustomTimesAdapter(this));
    }

    public static class CustomTimesAdapter extends ArrayAdapter<CustomTime> {
        private final Context mContext;
        private final ArrayList<CustomTime> mCustomTimes;

        public CustomTimesAdapter(Context context) {
            super(context, -1, new ArrayList<>(DomainFactory.getInstance().getCustomTimeFactory().getCustomTimes()));

            Assert.assertTrue(context != null);

            mContext = context;
            mCustomTimes = new ArrayList<>(DomainFactory.getInstance().getCustomTimeFactory().getCustomTimes());
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
}
