package com.krystianwsul.checkme.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;

import com.crashlytics.android.Crashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.utils.InstanceKey;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowNotificationGroupActivity extends AppCompatActivity implements GroupListFragment.GroupListListener {
    private static final String INSTANCES_KEY = "instanceKeys";

    public static Intent getIntent(Context context, ArrayList<InstanceKey> instanceKeys) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(!instanceKeys.isEmpty());

        Intent intent = new Intent(context, ShowNotificationGroupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putParcelableArrayListExtra(INSTANCES_KEY, instanceKeys);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_notification_group);

        Toolbar toolbar = (Toolbar) findViewById(R.id.show_notification_group_toolbar);
        Assert.assertTrue(toolbar != null);

        setSupportActionBar(toolbar);

        GroupListFragment showNotificationGroupList = (GroupListFragment) getSupportFragmentManager().findFragmentById(R.id.show_notification_group_list);
        Assert.assertTrue(showNotificationGroupList != null);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INSTANCES_KEY));
        ArrayList<InstanceKey> instanceKeys = intent.getParcelableArrayListExtra(INSTANCES_KEY);
        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(!instanceKeys.isEmpty());

        showNotificationGroupList.setInstanceKeys(instanceKeys);
    }

    @Override
    protected void onResume() {
        Crashlytics.log("ShowNotificationGroupActivity.onResume");

        super.onResume();
    }

    @Override
    public void onCreateGroupActionMode(ActionMode actionMode) {

    }

    @Override
    public void onDestroyGroupActionMode() {

    }
}