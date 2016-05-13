package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.gui.tasks.CreateChildTaskActivity;
import com.example.krystianwsul.organizator.gui.tasks.CreateRootTaskActivity;
import com.example.krystianwsul.organizator.gui.tasks.ShowTaskActivity;
import com.example.krystianwsul.organizator.loaders.ShowInstanceLoader;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.InstanceKey;

import junit.framework.Assert;

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
        intent.putExtra(INSTANCE_KEY, instanceKey);
        return intent;
    }

    public static Intent getNotificationIntent(Context context, InstanceKey instanceKey) {
        Intent intent = new Intent(context, ShowInstanceActivity.class);
        intent.putExtra(INSTANCE_KEY, instanceKey);
        intent.putExtra(SET_NOTIFIED_KEY, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private boolean mFirst = false;

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

        boolean showTask = (mData != null && !mData.Done && mData.TaskCurrent);
        menu.findItem(R.id.instance_menu_show_task).setVisible(showTask);

        boolean editTask = (mData != null && !mData.Done && mData.TaskCurrent);
        menu.findItem(R.id.instance_menu_edit_task).setVisible(editTask);

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
                    startActivity(CreateRootTaskActivity.getEditIntent(this, mData.InstanceKey.TaskId));
                else
                    startActivity(CreateChildTaskActivity.getEditIntent(this, mData.InstanceKey.TaskId));
                break;
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

        if (savedInstanceState == null)
            mFirst = true;

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INSTANCE_KEY));
        mInstanceKey = intent.getParcelableExtra(INSTANCE_KEY);
        Assert.assertTrue(mInstanceKey != null);

        GroupListFragment showInstanceList = (GroupListFragment) getSupportFragmentManager().findFragmentById(R.id.show_instance_list);
        Assert.assertTrue(showInstanceList != null);
        showInstanceList.setInstanceKey(mInstanceKey);

        getSupportLoaderManager().initLoader(0, null, this);
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
            DomainFactory.getDomainFactory(this).setInstanceNotified(data.DataId, data.InstanceKey);
        }

        mActionBar.setTitle(data.Name);

        if (TextUtils.isEmpty(data.DisplayText))
            mActionBar.setSubtitle(null);
        else
            mActionBar.setSubtitle(data.DisplayText);

        invalidateOptionsMenu();
    }

    private void setDone(boolean done) {
        DomainFactory.getDomainFactory(ShowInstanceActivity.this).setInstanceDone(mData.DataId, mData.InstanceKey, done);
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
}
