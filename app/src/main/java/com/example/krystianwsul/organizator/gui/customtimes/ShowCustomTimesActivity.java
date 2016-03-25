package com.example.krystianwsul.organizator.gui.customtimes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.loaders.ShowCustomTimesLoader;

import junit.framework.Assert;

public class ShowCustomTimesActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ShowCustomTimesLoader.Data> {
    private RecyclerView mShowTimesList;

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
        Assert.assertTrue(showTimesFab != null);

        showTimesFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(ShowCustomTimeActivity.getCreateIntent(ShowCustomTimesActivity.this));
            }
        });

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<ShowCustomTimesLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ShowCustomTimesLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<ShowCustomTimesLoader.Data> loader, ShowCustomTimesLoader.Data data) {
        mShowTimesList.setAdapter(new CustomTimesAdapter(data, this));
    }

    @Override
    public void onLoaderReset(Loader<ShowCustomTimesLoader.Data> loader) {
    }

    public static class CustomTimesAdapter extends RecyclerView.Adapter<CustomTimesAdapter.CustomTimeHolder> {
        private final ShowCustomTimesLoader.Data mData;
        private final Activity mActivity;

        public CustomTimesAdapter(ShowCustomTimesLoader.Data data, Activity activity) {
            Assert.assertTrue(data != null);
            Assert.assertTrue(activity != null);

            mData = data;
            mActivity = activity;
        }

        @Override
        public int getItemCount() {
            return mData.Entries.size();
        }

        @Override
        public CustomTimeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(mActivity);
            View showCustomTimesRow = layoutInflater.inflate(R.layout.row_show_custom_times, parent, false);

            TextView timesRowName = (TextView) showCustomTimesRow.findViewById(R.id.times_row_name);
            ImageView timesRowDelete = (ImageView) showCustomTimesRow.findViewById(R.id.times_row_delete);

            return new CustomTimeHolder(showCustomTimesRow, timesRowName, timesRowDelete);
        }

        @Override
        public void onBindViewHolder(final CustomTimeHolder customTimeHolder, int position) {
            ShowCustomTimesLoader.CustomTimeData customTimeData = mData.Entries.get(position);
            Assert.assertTrue(customTimeData != null);

            customTimeHolder.mTimesRowName.setText(customTimeData.Name);

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
                ShowCustomTimesLoader.CustomTimeData customTimeData = mData.Entries.get(getAdapterPosition());
                mActivity.startActivity(ShowCustomTimeActivity.getEditIntent(customTimeData.Id, mActivity));
            }

            public void onDeleteClick() {
                int position = getAdapterPosition();
                ShowCustomTimesLoader.CustomTimeData customTimeData = mData.Entries.get(position);

                DomainFactory.getDomainFactory(mActivity).setCustomTimeCurrent(mData.DataId, customTimeData.Id);

                mData.Entries.remove(customTimeData);
                notifyItemRemoved(position);
            }
        }
    }
}
