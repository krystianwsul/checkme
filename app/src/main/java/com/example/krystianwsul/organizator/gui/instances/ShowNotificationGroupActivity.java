package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.loaders.ShowNotificationGroupLoader;
import com.example.krystianwsul.organizator.utils.InstanceKey;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowNotificationGroupActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ShowNotificationGroupLoader.Data> {
    private static final String INSTANCES_KEY = "instanceKeys";

    private InstanceListFragment mShowNotificationGroupList;

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

        mShowNotificationGroupList = (InstanceListFragment) getSupportFragmentManager().findFragmentById(R.id.show_notification_group_list);
        Assert.assertTrue(mShowNotificationGroupList != null);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<ShowNotificationGroupLoader.Data> onCreateLoader(int id, Bundle args) {
        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INSTANCES_KEY));
        ArrayList<InstanceKey> instanceKeys = intent.getParcelableArrayListExtra(INSTANCES_KEY);
        Assert.assertTrue(instanceKeys != null);
        Assert.assertTrue(!instanceKeys.isEmpty());

        return new ShowNotificationGroupLoader(this, instanceKeys);
    }

    @Override
    public void onLoadFinished(Loader<ShowNotificationGroupLoader.Data> loader, ShowNotificationGroupLoader.Data data) {
        Assert.assertTrue(!data.InstanceAdapterDatas.isEmpty());
        mShowNotificationGroupList.setAdapter(data.DataId, data.InstanceAdapterDatas);
    }

    @Override
    public void onLoaderReset(Loader<ShowNotificationGroupLoader.Data> loader) {
    }
}