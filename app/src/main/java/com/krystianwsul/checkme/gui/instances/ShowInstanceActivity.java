package com.krystianwsul.checkme.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.tasks.CreateTaskActivity;
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity;
import com.krystianwsul.checkme.loaders.ShowInstanceLoader;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.Utils;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowInstanceActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ShowInstanceLoader.Data>, GroupListFragment.GroupListListener {
    private static final String INSTANCE_KEY = "instanceKey";
    private static final String SET_NOTIFIED_KEY = "setNotified";

    private ActionBar mActionBar;

    private InstanceKey mInstanceKey;

    private ShowInstanceLoader.Data mData;

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
        boolean check = (mData != null && !mData.Done);
        menu.findItem(R.id.instance_menu_check).setVisible(check);

        boolean uncheck = (mData != null && mData.Done);
        menu.findItem(R.id.instance_menu_uncheck).setVisible(uncheck);

        boolean editInstance = (mData != null && !mData.Done && mData.IsRootInstance);
        menu.findItem(R.id.instance_menu_edit_instance).setVisible(editInstance);

        menu.findItem(R.id.instance_menu_share).setVisible(mData != null);

        boolean showTask = (mData != null && !mData.Done && mData.TaskCurrent);
        menu.findItem(R.id.instance_menu_show_task).setVisible(showTask);

        boolean editTask = (mData != null && !mData.Done && mData.TaskCurrent);
        menu.findItem(R.id.instance_menu_edit_task).setVisible(editTask);

        boolean deleteTask = (mData != null && !mData.Done && mData.TaskCurrent);
        menu.findItem(R.id.instance_menu_delete_task).setVisible(deleteTask);

        menu.findItem(R.id.instance_menu_select_all).setVisible(mSelectAllVisible);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.instance_menu_check:
                Assert.assertTrue(mData != null);
                Assert.assertTrue(!mData.Done);

                setDone(true);
                break;
            case R.id.instance_menu_uncheck:
                Assert.assertTrue(mData != null);
                Assert.assertTrue(mData.Done);

                setDone(false);
                break;
            case R.id.instance_menu_edit_instance:
                Assert.assertTrue(mData != null);
                Assert.assertTrue(!mData.Done);
                Assert.assertTrue(mData.IsRootInstance);

                startActivity(EditInstanceActivity.getIntent(this, mData.InstanceKey));
                break;
            case R.id.instance_menu_share:
                Assert.assertTrue(mData != null);

                Utils.share(mData.Name, this);
                break;
            case R.id.instance_menu_show_task:
                Assert.assertTrue(mData != null);
                Assert.assertTrue(!mData.Done);
                Assert.assertTrue(mData.TaskCurrent);

                startActivity(ShowTaskActivity.getIntent(mData.InstanceKey.TaskId, this));
                break;
            case R.id.instance_menu_edit_task:
                Assert.assertTrue(mData != null);
                Assert.assertTrue(!mData.Done);
                Assert.assertTrue(mData.TaskCurrent);

                if (mData.IsRootTask)
                    startActivity(CreateTaskActivity.getEditIntent(this, mData.InstanceKey.TaskId));
                else
                    startActivity(CreateTaskActivity.getEditIntent(this, mData.InstanceKey.TaskId));
                break;
            case R.id.instance_menu_delete_task:
                Assert.assertTrue(mData != null);
                Assert.assertTrue(!mData.Done);
                Assert.assertTrue(mData.TaskCurrent);

                ArrayList<Integer> dataIds = new ArrayList<>();
                dataIds.add(mData.DataId);

                getSupportLoaderManager().destroyLoader(0);
                mGroupListFragment.destroyLoader();

                DomainFactory.getDomainFactory(this).setTaskEndTimeStamp(this, dataIds, mData.InstanceKey.TaskId);

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
    protected void onResume() {
        MyCrashlytics.log("ShowInstanceActivity.onResume");

        super.onResume();
    }

    @Override
    public Loader<ShowInstanceLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ShowInstanceLoader(this, mInstanceKey);
    }

    @Override
    public void onLoadFinished(Loader<ShowInstanceLoader.Data> loader, final ShowInstanceLoader.Data data) {
        mData = data;

        Intent intent = getIntent();

        if (intent.getBooleanExtra(SET_NOTIFIED_KEY, false) && mFirst) {
            mFirst = false;
            DomainFactory.getDomainFactory(this).setInstanceNotified(this, data.DataId, data.InstanceKey);
        }

        mActionBar.setTitle(data.Name);

        if (TextUtils.isEmpty(data.DisplayText))
            mActionBar.setSubtitle(null);
        else
            mActionBar.setSubtitle(data.DisplayText);

        invalidateOptionsMenu();
    }

    private void setDone(boolean done) {
        DomainFactory.getDomainFactory(ShowInstanceActivity.this).setInstanceDone(this, mData.DataId, mData.InstanceKey, done);
        mData.Done = done;

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
