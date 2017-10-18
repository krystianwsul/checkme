package com.krystianwsul.checkme.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.AbstractActivity;
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment;
import com.krystianwsul.checkme.loaders.ShowGroupLoader;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

public class ShowGroupActivity extends AbstractActivity implements LoaderManager.LoaderCallbacks<ShowGroupLoader.Data>, GroupListFragment.GroupListListener {
    private TimeStamp mTimeStamp;

    private static final String TIME_KEY = "time";

    private ActionBar mActionBar;

    private GroupListFragment mGroupListFragment;

    private boolean mSelectAllVisible = false;

    public static Intent getIntent(ExactTimeStamp exactTimeStamp, Context context) {
        Intent intent = new Intent(context, ShowGroupActivity.class);
        intent.putExtra(TIME_KEY, exactTimeStamp.getLong());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_group);

        Toolbar toolbar = findViewById(R.id.toolbar);
        Assert.assertTrue(toolbar != null);

        setSupportActionBar(toolbar);

        mActionBar = getSupportActionBar();
        Assert.assertTrue(mActionBar != null);

        mActionBar.setTitle(null);

        FloatingActionButton showGroupFab = findViewById(R.id.show_group_fab);
        Assert.assertTrue(showGroupFab != null);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(TIME_KEY));
        long time = intent.getLongExtra(TIME_KEY, -1);
        Assert.assertTrue(time != -1);
        mTimeStamp = new TimeStamp(time);

        mGroupListFragment = (GroupListFragment) getSupportFragmentManager().findFragmentById(R.id.show_group_list);
        if (mGroupListFragment == null) {
            mGroupListFragment = GroupListFragment.Companion.newInstance();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.show_group_list, mGroupListFragment)
                    .commit();
        }

        mGroupListFragment.setFab(showGroupFab);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<ShowGroupLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ShowGroupLoader(this, mTimeStamp);
    }

    @Override
    public void onLoadFinished(Loader<ShowGroupLoader.Data> loader, ShowGroupLoader.Data data) {
        Assert.assertTrue(data != null);

        mActionBar.setTitle(data.mDisplayText);

        if (data.mDataWrapper == null) {
            finish();

            return;
        }

        mGroupListFragment.setTimeStamp(mTimeStamp, data.DataId, data.mDataWrapper);
    }

    @Override
    public void onLoaderReset(Loader<ShowGroupLoader.Data> loader) {
    }

    @Override
    public void onCreateGroupActionMode(@NonNull ActionMode actionMode) {

    }

    @Override
    public void onDestroyGroupActionMode() {

    }

    @Override
    public void setGroupSelectAllVisibility(Integer position, boolean selectAllVisible) {
        mSelectAllVisible = selectAllVisible;

        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_select_all, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_select_all).setVisible(mSelectAllVisible);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Assert.assertTrue(item.getItemId() == R.id.action_select_all);
        Assert.assertTrue(mGroupListFragment != null);

        mGroupListFragment.selectAll();

        return true;
    }
}