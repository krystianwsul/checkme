package com.krystianwsul.checkme.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.AbstractActivity;
import com.krystianwsul.checkme.loaders.ShowNotificationGroupLoader;
import com.krystianwsul.checkme.utils.InstanceKey;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ShowNotificationGroupActivity extends AbstractActivity implements GroupListFragment.GroupListListener, LoaderManager.LoaderCallbacks<ShowNotificationGroupLoader.Data> {
    private static final String INSTANCES_KEY = "instanceKeys";

    private Set<InstanceKey> mInstanceKeys;

    private GroupListFragment mGroupListFragment;

    private boolean mSelectAllVisible = false;

    @NonNull
    public static Intent getIntent(@NonNull Context context, @NonNull ArrayList<InstanceKey> instanceKeys) {
        Assert.assertTrue(!instanceKeys.isEmpty());

        Intent intent = new Intent(context, ShowNotificationGroupActivity.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putParcelableArrayListExtra(INSTANCES_KEY, instanceKeys);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_notification_group);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        Assert.assertTrue(toolbar != null);

        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        Assert.assertTrue(actionBar != null);

        actionBar.setTitle(null);

        mGroupListFragment = (GroupListFragment) getSupportFragmentManager().findFragmentById(R.id.show_notification_group_list);
        if (mGroupListFragment == null) {
            mGroupListFragment = GroupListFragment.newInstance();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.show_notification_group_list, mGroupListFragment)
                    .commit();
        }

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INSTANCES_KEY));
        ArrayList<InstanceKey> instanceKeys = intent.getParcelableArrayListExtra(INSTANCES_KEY);
        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(!instanceKeys.isEmpty());

        mInstanceKeys = new HashSet<>(instanceKeys);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<ShowNotificationGroupLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ShowNotificationGroupLoader(this, mInstanceKeys);
    }

    @Override
    public void onLoadFinished(Loader<ShowNotificationGroupLoader.Data> loader, ShowNotificationGroupLoader.Data data) {
        Assert.assertTrue(data != null);

        mGroupListFragment.setInstanceKeys(mInstanceKeys, data.DataId, data.mDataWrapper);
    }

    @Override
    public void onLoaderReset(Loader<ShowNotificationGroupLoader.Data> loader) {

    }

    @Override
    public void onCreateGroupActionMode(ActionMode actionMode) {

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