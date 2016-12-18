package com.krystianwsul.checkme.gui.projects;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.AbstractActivity;
import com.krystianwsul.checkme.gui.friends.UserListFragment;
import com.krystianwsul.checkme.loaders.ShowProjectLoader;

import junit.framework.Assert;

public class ShowProjectActivity extends AbstractActivity implements LoaderManager.LoaderCallbacks<ShowProjectLoader.Data>, UserListFragment.Listener {
    private static final String PROJECT_ID_KEY = "projectId";

    private String mProjectId;

    private ActionBar mActionBar;

    private boolean mSelectAllVisible = false;

    public static Intent newIntent(@NonNull Context context, @NonNull String projectId) {
        Assert.assertTrue(!TextUtils.isEmpty(projectId));

        Intent intent = new Intent(context, ShowProjectActivity.class);
        intent.putExtra(PROJECT_ID_KEY, projectId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_project);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        Assert.assertTrue(toolbar != null);

        setSupportActionBar(toolbar);

        mActionBar = getSupportActionBar();
        Assert.assertTrue(mActionBar != null);

        mActionBar.setTitle(null);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(PROJECT_ID_KEY));

        mProjectId = intent.getStringExtra(PROJECT_ID_KEY);
        Assert.assertTrue(mProjectId != null);

        UserListFragment userListFragment = (UserListFragment) getSupportFragmentManager().findFragmentById(R.id.show_project_frame);
        if (userListFragment == null) {
            userListFragment = UserListFragment.newProjectInstance(mProjectId);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.show_project_frame, userListFragment)
                    .commit();
        }

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.show_project_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.project_menu_select_all).setVisible(mSelectAllVisible);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.project_menu_select_all: {
                UserListFragment userListFragment = (UserListFragment) getSupportFragmentManager().findFragmentById(R.id.show_project_frame);
                Assert.assertTrue(userListFragment != null);

                userListFragment.selectAll();

                break;
            }
            default:
                throw new UnsupportedOperationException();
        }
        return true;
    }

    @Override
    public Loader<ShowProjectLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ShowProjectLoader(this, mProjectId);
    }

    @Override
    public void onLoadFinished(Loader<ShowProjectLoader.Data> loader, ShowProjectLoader.Data data) {
        Assert.assertTrue(data != null);

        mActionBar.setTitle(data.mName);

        invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<ShowProjectLoader.Data> loader) {
    }

    @Override
    public void onCreateUserActionMode(@NonNull ActionMode actionMode) {

    }

    @Override
    public void onDestroyUserActionMode() {

    }

    @Override
    public void setUserSelectAllVisibility(boolean selectAllVisible) {
        mSelectAllVisible = selectAllVisible;

        invalidateOptionsMenu();
    }
}
