package com.krystianwsul.checkme.gui.projects;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.AbstractActivity;
import com.krystianwsul.checkme.gui.DiscardDialogFragment;
import com.krystianwsul.checkme.gui.friends.UserListFragment;
import com.krystianwsul.checkme.loaders.ShowProjectLoader;

import junit.framework.Assert;

public class ShowProjectActivity extends AbstractActivity implements LoaderManager.LoaderCallbacks<ShowProjectLoader.Data> {
    private static final String PROJECT_ID_KEY = "projectId";

    private static final String DISCARD_TAG = "discard";

    @Nullable
    private String mProjectId;

    private TextInputLayout mToolbarLayout;
    private EditText mToolbarEditText;

    private ShowProjectLoader.Data mData;

    private UserListFragment mUserListFragment;

    @Nullable
    private Bundle mSavedInstanceState;

    private final DiscardDialogFragment.DiscardDialogListener mDiscardDialogListener = this::finish;

    public static Intent newIntent(@NonNull Context context, @NonNull String projectId) {
        Assert.assertTrue(!TextUtils.isEmpty(projectId));

        Intent intent = new Intent(context, ShowProjectActivity.class);
        intent.putExtra(PROJECT_ID_KEY, projectId);
        return intent;
    }

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, ShowProjectActivity.class);
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

                if (updateError())
                    break;

                getSupportLoaderManager().destroyLoader(0);

                mUserListFragment.save(mToolbarEditText.getText().toString());

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

        mSavedInstanceState = savedInstanceState;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        Assert.assertTrue(toolbar != null);

        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        Assert.assertTrue(actionBar != null);

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

        mToolbarLayout = (TextInputLayout) findViewById(R.id.toolbarLayout);
        Assert.assertTrue(mToolbarLayout != null);

        mToolbarEditText = (EditText) findViewById(R.id.toolbarEditText);
        Assert.assertTrue(mToolbarEditText != null);

        FloatingActionButton showProjectFab = (FloatingActionButton) findViewById(R.id.show_project_fab);
        Assert.assertTrue(showProjectFab != null);

        mToolbarEditText.addTextChangedListener(new TextWatcher() {
            private boolean mSkip = true;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mSkip) {
                    mSkip = false;
                    return;
                }

                updateError();
            }
        });

        Intent intent = getIntent();
        if (intent.hasExtra(PROJECT_ID_KEY)) {
            mProjectId = intent.getStringExtra(PROJECT_ID_KEY);
            Assert.assertTrue(!TextUtils.isEmpty(mProjectId));
        }

        mUserListFragment = (UserListFragment) getSupportFragmentManager().findFragmentById(R.id.show_project_frame);
        if (mUserListFragment == null) {
            mUserListFragment = UserListFragment.newInstance();

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.show_project_frame, mUserListFragment)
                    .commit();
        }
        mUserListFragment.setFab(showProjectFab);

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

        if (mSavedInstanceState == null) {
            mToolbarEditText.setText(data.mName);
        } else {
            mSavedInstanceState = null;
        }

        mToolbarLayout.setVisibility(View.VISIBLE);
        mToolbarLayout.setHintAnimationEnabled(true);

        invalidateOptionsMenu();

        mUserListFragment.initialize(mProjectId, data);
    }

    @Override
    public void onLoaderReset(Loader<ShowProjectLoader.Data> loader) {

    }

    @Override
    public void onBackPressed() {
        if (tryClose())
            super.onBackPressed();
    }

    private boolean tryClose() {
        if (dataChanged()) {
            DiscardDialogFragment discardDialogFragment = DiscardDialogFragment.newInstance();
            discardDialogFragment.setDiscardDialogListener(mDiscardDialogListener);
            discardDialogFragment.show(getSupportFragmentManager(), DISCARD_TAG);

            return false;
        } else {
            return true;
        }
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean dataChanged() {
        if (mData == null)
            return false;

        if (TextUtils.isEmpty(mToolbarEditText.getText()) != TextUtils.isEmpty(mData.mName))
            return true;

        if (!TextUtils.isEmpty(mToolbarEditText.getText()) && !mToolbarEditText.getText().toString().equals(mData.mName))
            return true;

        return mUserListFragment.dataChanged();
    }

    private boolean updateError() {
        Assert.assertTrue(mData != null);
        Assert.assertTrue(mToolbarEditText != null);
        Assert.assertTrue(mToolbarLayout != null);

        if (TextUtils.isEmpty(mToolbarEditText.getText())) {
            mToolbarLayout.setError(getString(R.string.nameError));

            return true;
        } else {
            mToolbarLayout.setError(null);

            return false;
        }
    }
}
