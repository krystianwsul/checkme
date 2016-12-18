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
import com.krystianwsul.checkme.gui.DiscardDialogFragment;
import com.krystianwsul.checkme.gui.friends.UserListFragment;
import com.krystianwsul.checkme.loaders.ShowProjectLoader;

import junit.framework.Assert;

public class ShowProjectActivity extends AbstractActivity implements LoaderManager.LoaderCallbacks<ShowProjectLoader.Data>, UserListFragment.Listener {
    private static final String PROJECT_ID_KEY = "projectId";

    private static final String DISCARD_TAG = "discard";

    private String mProjectId;

    private ActionBar mActionBar;

    private ShowProjectLoader.Data mData;

    private UserListFragment mUserListFragment;

    private final DiscardDialogFragment.DiscardDialogListener mDiscardDialogListener = this::finish;

    public static Intent newIntent(@NonNull Context context, @NonNull String projectId) {
        Assert.assertTrue(!TextUtils.isEmpty(projectId));

        Intent intent = new Intent(context, ShowProjectActivity.class);
        intent.putExtra(PROJECT_ID_KEY, projectId);
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_save, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_save).setVisible(mData != null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save: {
                Assert.assertTrue(mData != null);

                mUserListFragment.save(mData.DataId);

                finish();

                break;
            }
            case android.R.id.home: {
                if (tryClose())
                    finish();

                break;
            }
            default:
                throw new UnsupportedOperationException();
        }
        return true;
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

        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(PROJECT_ID_KEY));

        mProjectId = intent.getStringExtra(PROJECT_ID_KEY);
        Assert.assertTrue(mProjectId != null);

        mUserListFragment = (UserListFragment) getSupportFragmentManager().findFragmentById(R.id.show_project_frame);
        if (mUserListFragment == null) {
            mUserListFragment = UserListFragment.newProjectInstance(mProjectId);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.show_project_frame, mUserListFragment)
                    .commit();
        }

        DiscardDialogFragment discardDialogFragment = (DiscardDialogFragment) getSupportFragmentManager().findFragmentByTag(DISCARD_TAG);
        if (discardDialogFragment != null)
            discardDialogFragment.setDiscardDialogListener(mDiscardDialogListener);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<ShowProjectLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ShowProjectLoader(this, mProjectId);
    }

    @Override
    public void onLoadFinished(Loader<ShowProjectLoader.Data> loader, ShowProjectLoader.Data data) {
        Assert.assertTrue(data != null);

        mData = data;

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

    }

    @Override
    public void onBackPressed() {
        if (tryClose())
            super.onBackPressed();
    }

    private boolean tryClose() {
        if (mUserListFragment.dataChanged()) {
            DiscardDialogFragment discardDialogFragment = DiscardDialogFragment.newInstance();
            discardDialogFragment.setDiscardDialogListener(mDiscardDialogListener);
            discardDialogFragment.show(getSupportFragmentManager(), DISCARD_TAG);

            return false;
        } else {
            return true;
        }
    }
}
