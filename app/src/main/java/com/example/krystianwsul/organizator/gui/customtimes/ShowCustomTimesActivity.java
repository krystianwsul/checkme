package com.example.krystianwsul.organizator.gui.customtimes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.CustomTime;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowCustomTimesActivity extends AppCompatActivity {
    private RecyclerView mShowTimesList;
    private DomainFactory mDomainFactory;

    public static Intent getIntent(Context context) {
        return new Intent(context, ShowCustomTimesActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_custom_times);

        mShowTimesList = (RecyclerView) findViewById(R.id.show_times_list);
        mShowTimesList.setLayoutManager(new LinearLayoutManager(this));

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

        mDomainFactory = DomainFactory.getDomainFactory(this);
        Assert.assertTrue(mDomainFactory != null);

        mShowTimesList.setAdapter(new CustomTimesAdapter(mDomainFactory, this));
    }

    public class CustomTimesAdapter extends RecyclerView.Adapter<CustomTimesAdapter.CustomTimeHolder> {
        private final Activity mActivity;
        private final ArrayList<CustomTime> mCustomTimes;

        public CustomTimesAdapter(DomainFactory domainFactory, Activity activity) {
            Assert.assertTrue(domainFactory != null);
            Assert.assertTrue(activity != null);

            mActivity = activity;
            mCustomTimes = new ArrayList<>(domainFactory.getCustomTimeFactory().getCurrentCustomTimes());
        }

        @Override
        public int getItemCount() {
            return mCustomTimes.size();
        }

        @Override
        public CustomTimeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(mActivity);
            View showCustomTimesRow = layoutInflater.inflate(R.layout.show_custom_times_row, parent, false);

            TextView timesRowName = (TextView) showCustomTimesRow.findViewById(R.id.times_row_name);
            ImageView timesRowDelete = (ImageView) showCustomTimesRow.findViewById(R.id.times_row_delete);

            return new CustomTimeHolder(showCustomTimesRow, timesRowName, timesRowDelete);
        }

        @Override
        public void onBindViewHolder(final CustomTimeHolder customTimeHolder, int position) {
            CustomTime customTime = mCustomTimes.get(position);
            Assert.assertTrue(customTime != null);

            customTimeHolder.mTimesRowName.setText(customTime.getName());

            customTimeHolder.mShowCustomTimeRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    customTimeHolder.onRowClick();
                }
            });

            customTimeHolder.mTimesRowDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    customTimeHolder.onDeleteClick();
                }
            });
        }

        public class CustomTimeHolder extends RecyclerView.ViewHolder {
            public final View mShowCustomTimeRow;
            public final TextView mTimesRowName;
            public final ImageView mTimesRowDelete;

            public CustomTimeHolder(View showCustomTimesRow, TextView timesRowName, ImageView timesRowDelete) {
                super(showCustomTimesRow);

                Assert.assertTrue(timesRowName != null);
                Assert.assertTrue(timesRowDelete != null);

                mShowCustomTimeRow = showCustomTimesRow;
                mTimesRowName = timesRowName;
                mTimesRowDelete = timesRowDelete;
            }

            public void onRowClick() {
                CustomTime customTime = mCustomTimes.get(getAdapterPosition());
                mActivity.startActivity(ShowCustomTimeActivity.getEditIntent(customTime, mActivity));
            }

            public void onDeleteClick() {
                int position = getAdapterPosition();
                CustomTime customTime = mCustomTimes.get(position);
                customTime.setCurrent();

                Assert.assertTrue(mDomainFactory != null);
                mDomainFactory.save();

                mCustomTimes.remove(customTime);
                notifyItemRemoved(position);
            }
        }
    }
}
