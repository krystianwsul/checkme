package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.gui.tasks.ShowTaskActivity;
import com.example.krystianwsul.organizator.loaders.ShowInstanceLoader;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.InstanceKey;

import junit.framework.Assert;

public class ShowInstanceActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ShowInstanceLoader.Data> {
    private static final String INSTANCE_KEY = "instanceKey";
    private static final String SET_NOTIFIED_KEY = "setNotified";

    private TextView mShowInstanceName;
    private TextView mShowInstanceDetails;
    private CheckBox mCheckBox;

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
        boolean editInstance = (mData != null && !mData.Done);
        menu.findItem(R.id.instance_menu_edit_instance).setVisible(editInstance);

        boolean showTask = (mData != null && !mData.Done && mData.TaskCurrent);
        menu.findItem(R.id.instance_menu_show_task).setVisible(showTask);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.instance_menu_edit_instance:
                Assert.assertTrue(mData != null);
                Intent intent = EditInstanceActivity.getIntent(this, mData.InstanceKey);
                startActivity(intent);
                break;
            case R.id.instance_menu_show_task:
                Assert.assertTrue(mData != null);
                startActivity(ShowTaskActivity.getIntent(mData.InstanceKey.TaskId, this));
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

        if (savedInstanceState == null)
            mFirst = true;

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INSTANCE_KEY));
        mInstanceKey = intent.getParcelableExtra(INSTANCE_KEY);
        Assert.assertTrue(mInstanceKey != null);

        mShowInstanceName = (TextView) findViewById(R.id.show_instance_name);
        Assert.assertTrue(mShowInstanceName != null);

        mShowInstanceDetails = (TextView) findViewById(R.id.show_instance_details);
        Assert.assertTrue(mShowInstanceDetails != null);

        GroupListFragment showInstanceList = (GroupListFragment) getSupportFragmentManager().findFragmentById(R.id.show_instance_list);
        Assert.assertTrue(showInstanceList != null);
        showInstanceList.setInstanceKey(mInstanceKey);

        mCheckBox = (CheckBox) findViewById(R.id.show_instance_checkbox);

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

        mShowInstanceName.setText(data.Name);

        mCheckBox.setChecked(data.Done);

        String scheduleText = data.DisplayText;
        if (TextUtils.isEmpty(scheduleText))
            mShowInstanceDetails.setVisibility(View.GONE);
        else
            mShowInstanceDetails.setText(scheduleText);

        mCheckBox.setOnClickListener(v -> {
            boolean isChecked = mCheckBox.isChecked();

            DomainFactory.getDomainFactory(ShowInstanceActivity.this).setInstanceDone(data.DataId, data.InstanceKey, isChecked);
            data.Done = isChecked;

            TickService.startService(ShowInstanceActivity.this);

            invalidateOptionsMenu();
        });

        invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<ShowInstanceLoader.Data> loader) {
    }
}
