package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.loaders.ShowGroupLoader;
import com.example.krystianwsul.organizator.utils.time.ExactTimeStamp;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

public class ShowGroupActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ShowGroupLoader.Data> {
    private TimeStamp mTimeStamp;
    private TextView mShowGroupName;

    private static final String TIME_KEY = "time";

    public static Intent getIntent(ExactTimeStamp exactTimeStamp, Context context) {
        Intent intent = new Intent(context, ShowGroupActivity.class);
        intent.putExtra(TIME_KEY, exactTimeStamp.getLong());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_group);

        Toolbar toolbar = (Toolbar) findViewById(R.id.show_group_toolbar);
        Assert.assertTrue(toolbar != null);

        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(TIME_KEY));
        long time = intent.getLongExtra(TIME_KEY, -1);
        Assert.assertTrue(time != -1);
        mTimeStamp = new TimeStamp(time);

        mShowGroupName = (TextView) findViewById(R.id.show_group_name);

        GroupListFragment showGroupList = (GroupListFragment) getSupportFragmentManager().findFragmentById(R.id.show_group_list);
        Assert.assertTrue(showGroupList != null);
        showGroupList.setTimeStamp(mTimeStamp);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<ShowGroupLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ShowGroupLoader(this, mTimeStamp);
    }

    @Override
    public void onLoadFinished(Loader<ShowGroupLoader.Data> loader, ShowGroupLoader.Data data) {
        mShowGroupName.setText(data.DisplayText);

        if (!data.HasInstances)
            finish();
    }

    @Override
    public void onLoaderReset(Loader<ShowGroupLoader.Data> loader) {
    }
}