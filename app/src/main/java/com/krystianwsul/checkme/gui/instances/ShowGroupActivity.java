package com.krystianwsul.checkme.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.loaders.ShowGroupLoader;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

public class ShowGroupActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ShowGroupLoader.Data>, GroupListFragment.GroupListListener {
    private TimeStamp mTimeStamp;

    private static final String TIME_KEY = "time";

    private ActionBar mActionBar;

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

        mActionBar = getSupportActionBar();
        Assert.assertTrue(mActionBar != null);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(TIME_KEY));
        long time = intent.getLongExtra(TIME_KEY, -1);
        Assert.assertTrue(time != -1);
        mTimeStamp = new TimeStamp(time);

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
        mActionBar.setTitle(data.DisplayText);

        if (!data.HasInstances)
            finish();
    }

    @Override
    public void onLoaderReset(Loader<ShowGroupLoader.Data> loader) {
    }

    @Override
    public void onCreateGroupActionMode(ActionMode actionMode) {

    }

    @Override
    public void onDestroyGroupActionMode() {

    }
}