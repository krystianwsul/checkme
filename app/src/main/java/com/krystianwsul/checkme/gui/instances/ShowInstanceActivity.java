package com.krystianwsul.checkme.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.AbstractActivity;
import com.krystianwsul.checkme.gui.tasks.CreateTaskActivity;
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity;
import com.krystianwsul.checkme.loaders.ShowInstanceLoader;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.Utils;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowInstanceActivity extends AbstractActivity implements LoaderManager.LoaderCallbacks<ShowInstanceLoader.Data>, GroupListFragment.GroupListListener {
    private static final String INSTANCE_KEY = "instanceKey";
    private static final String SET_NOTIFIED_KEY = "setNotified";

    private ActionBar mActionBar;

    private InstanceKey mInstanceKey;

    private Integer mDataId;
    private ShowInstanceLoader.InstanceData mInstanceData;

    public static Intent getIntent(Context context, InstanceKey instanceKey) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instanceKey != null);

        Intent intent = new Intent(context, ShowInstanceActivity.class);
        intent.putExtra(INSTANCE_KEY, (Parcelable) instanceKey);
        return intent;
    }

    public static Intent getNotificationIntent(Context context, InstanceKey instanceKey) {
        Intent intent = new Intent(context, ShowInstanceActivity.class);
        intent.putExtra(INSTANCE_KEY, (Parcelable) instanceKey);
        intent.putExtra(SET_NOTIFIED_KEY, true);
        return intent;
    }

    private boolean mFirst = false;

    private GroupListFragment mGroupListFragment;

    private boolean mSelectAllVisible = false;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.show_instance_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean check = (mInstanceData != null && !mInstanceData.Done);
        menu.findItem(R.id.instance_menu_check).setVisible(check);

        boolean uncheck = (mInstanceData != null && mInstanceData.Done);
        menu.findItem(R.id.instance_menu_uncheck).setVisible(uncheck);

        boolean editInstance = (mInstanceData != null && !mInstanceData.Done && mInstanceData.IsRootInstance);
        menu.findItem(R.id.instance_menu_edit_instance).setVisible(editInstance);

        menu.findItem(R.id.instance_menu_share).setVisible(mInstanceData != null);

        boolean showTask = (mInstanceData != null && !mInstanceData.Done && mInstanceData.TaskCurrent);
        menu.findItem(R.id.instance_menu_show_task).setVisible(showTask);

        boolean editTask = (mInstanceData != null && !mInstanceData.Done && mInstanceData.TaskCurrent);
        menu.findItem(R.id.instance_menu_edit_task).setVisible(editTask);

        boolean deleteTask = (mInstanceData != null && !mInstanceData.Done && mInstanceData.TaskCurrent);
        menu.findItem(R.id.instance_menu_delete_task).setVisible(deleteTask);

        menu.findItem(R.id.instance_menu_select_all).setVisible(mSelectAllVisible);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.instance_menu_check:
                Assert.assertTrue(mInstanceData != null);
                Assert.assertTrue(!mInstanceData.Done);

                setDone(true);
                break;
            case R.id.instance_menu_uncheck:
                Assert.assertTrue(mInstanceData != null);
                Assert.assertTrue(mInstanceData.Done);

                setDone(false);
                break;
            case R.id.instance_menu_edit_instance:
                Assert.assertTrue(mInstanceData != null);
                Assert.assertTrue(!mInstanceData.Done);
                Assert.assertTrue(mInstanceData.IsRootInstance);

                startActivity(EditInstanceActivity.getIntent(this, mInstanceData.InstanceKey));
                break;
            case R.id.instance_menu_share:
                Assert.assertTrue(mInstanceData != null);
                Assert.assertTrue(mGroupListFragment != null);

                String shareData = mGroupListFragment.getShareData();
                if (TextUtils.isEmpty(shareData))
                    Utils.share(mInstanceData.Name, this);
                else
                    Utils.share(mInstanceData.Name + "\n" + shareData, this);

                break;
            case R.id.instance_menu_show_task:
                Assert.assertTrue(mInstanceData != null);
                Assert.assertTrue(!mInstanceData.Done);
                Assert.assertTrue(mInstanceData.TaskCurrent);

                startActivity(ShowTaskActivity.newIntent(this, mInstanceData.InstanceKey.mTaskKey));
                break;
            case R.id.instance_menu_edit_task:
                Assert.assertTrue(mInstanceData != null);
                Assert.assertTrue(!mInstanceData.Done);
                Assert.assertTrue(mInstanceData.TaskCurrent);

                if (mInstanceData.IsRootTask)
                    startActivity(CreateTaskActivity.getEditIntent(this, mInstanceData.InstanceKey.mTaskKey));
                else
                    startActivity(CreateTaskActivity.getEditIntent(this, mInstanceData.InstanceKey.mTaskKey));
                break;
            case R.id.instance_menu_delete_task:
                Assert.assertTrue(mInstanceData != null);
                Assert.assertTrue(!mInstanceData.Done);
                Assert.assertTrue(mInstanceData.TaskCurrent);

                ArrayList<Integer> dataIds = new ArrayList<>();
                dataIds.add(mDataId);

                getSupportLoaderManager().destroyLoader(0);
                mGroupListFragment.destroyLoader();

                DomainFactory.getDomainFactory(this).setTaskEndTimeStamp(this, dataIds, mInstanceData.InstanceKey.mTaskKey);

                TickService.startService(this);

                finish();
                break;
            case R.id.instance_menu_select_all: {
                Assert.assertTrue(mGroupListFragment != null);

                mGroupListFragment.selectAll();

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
        setContentView(R.layout.activity_show_instance);

        Toolbar toolbar = (Toolbar) findViewById(R.id.show_instance_toolbar);
        Assert.assertTrue(toolbar != null);

        setSupportActionBar(toolbar);

        mActionBar = getSupportActionBar();
        Assert.assertTrue(mActionBar != null);

        mActionBar.setTitle(null);

        if (savedInstanceState == null)
            mFirst = true;

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INSTANCE_KEY));
        mInstanceKey = intent.getParcelableExtra(INSTANCE_KEY);
        Assert.assertTrue(mInstanceKey != null);

        mGroupListFragment = (GroupListFragment) getSupportFragmentManager().findFragmentById(R.id.show_instance_list);
        Assert.assertTrue(mGroupListFragment != null);

        mGroupListFragment.setInstanceKey(mInstanceKey);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<ShowInstanceLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ShowInstanceLoader(this, mInstanceKey);
    }

    @Override
    public void onLoadFinished(Loader<ShowInstanceLoader.Data> loader, final ShowInstanceLoader.Data data) {
        if (data.mInstanceData == null) {
            finish();
            return;
        }

        mDataId = data.DataId;
        mInstanceData = data.mInstanceData;

        Intent intent = getIntent();

        if (intent.getBooleanExtra(SET_NOTIFIED_KEY, false) && mFirst) {
            mFirst = false;
            DomainFactory.getDomainFactory(this).setInstanceNotified(this, data.DataId, mInstanceData.InstanceKey);
        }

        mActionBar.setTitle(mInstanceData.Name);

        if (TextUtils.isEmpty(mInstanceData.DisplayText))
            mActionBar.setSubtitle(null);
        else
            mActionBar.setSubtitle(mInstanceData.DisplayText);

        invalidateOptionsMenu();
    }

    private void setDone(boolean done) {
        DomainFactory.getDomainFactory(ShowInstanceActivity.this).setInstanceDone(this, mDataId, mInstanceData.InstanceKey, done);
        mInstanceData.Done = done;

        TickService.startService(ShowInstanceActivity.this);

        invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<ShowInstanceLoader.Data> loader) {
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
}
