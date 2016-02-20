package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.loaders.ShowNotificationGroupLoader;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowNotificationGroupActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ShowNotificationGroupLoader.Data> {
    private static final String INSTANCES_KEY = "instances";

    private RecyclerView mShowNotificationGroupList;

    private Bundle mSavedInstanceState;

    public static Intent getIntent(Context context, ArrayList<Bundle> bundles) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(bundles != null);
        Assert.assertTrue(!bundles.isEmpty());

        Intent intent = new Intent(context, ShowNotificationGroupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putParcelableArrayListExtra(INSTANCES_KEY, bundles);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_notification_group);

        mSavedInstanceState = savedInstanceState;

        mShowNotificationGroupList = (RecyclerView) findViewById(R.id.show_notification_group_list);
        mShowNotificationGroupList.setLayoutManager(new LinearLayoutManager(this));

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<ShowNotificationGroupLoader.Data> onCreateLoader(int id, Bundle args) {
        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INSTANCES_KEY));
        ArrayList<Bundle> bundles = intent.getParcelableArrayListExtra(INSTANCES_KEY);
        Assert.assertTrue(bundles != null);
        Assert.assertTrue(!bundles.isEmpty());

        ArrayList<ShowNotificationGroupLoader.InstanceKey> instanceKeys = new ArrayList<>();
        for (Bundle bundle : bundles) {
            int taskId = NewInstanceData.getTaskId(bundle);

            Date scheduleDate = NewInstanceData.getScheduleDate(bundle);
            Assert.assertTrue(scheduleDate != null);

            Integer scheduleCustomTimeId = NewInstanceData.getScheduleCustomTimeId(bundle);
            HourMinute scheduleHourMinute = NewInstanceData.getScheduleHourMinute(bundle);
            Assert.assertTrue((scheduleCustomTimeId == null) != (scheduleHourMinute == null));

            instanceKeys.add(new ShowNotificationGroupLoader.InstanceKey(taskId, scheduleDate, scheduleCustomTimeId, scheduleHourMinute));
        }

        return new ShowNotificationGroupLoader(this, instanceKeys);
    }

    @Override
    public void onLoadFinished(Loader<ShowNotificationGroupLoader.Data> loader, ShowNotificationGroupLoader.Data data) {
        Assert.assertTrue(!data.InstanceDatas.isEmpty());

        if (mSavedInstanceState == null) {
            ArrayList<ShowNotificationGroupLoader.InstanceKey> instanceKeys = new ArrayList<>();
            for (ShowNotificationGroupLoader.InstanceData instanceData : data.InstanceDatas)
                instanceKeys.add(new ShowNotificationGroupLoader.InstanceKey(instanceData.TaskId, instanceData.ScheduleDate, instanceData.ScheduleCustomTimeId, instanceData.ScheduleHourMinute));

            DomainFactory.getDomainFactory(this).setInstanceKeysNotified(data.DataId, instanceKeys);
        }

        ArrayList<InstanceAdapter.Data> datas = new ArrayList<>();
        for (ShowNotificationGroupLoader.InstanceData instanceData : data.InstanceDatas)
            datas.add(new InstanceAdapter.Data(instanceData.Done, instanceData.Name, instanceData.HasChildren, instanceData.TaskId, instanceData.ScheduleDate, instanceData.ScheduleCustomTimeId, instanceData.ScheduleHourMinute, instanceData.DisplayText));

        mShowNotificationGroupList.setAdapter(new InstanceAdapter(this, data.DataId, datas));
    }

    @Override
    public void onLoaderReset(Loader<ShowNotificationGroupLoader.Data> loader) {
    }
}