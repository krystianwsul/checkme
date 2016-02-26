package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.loaders.ShowInstanceLoader;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowInstanceActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ShowInstanceLoader.Data> {
    private static final String INTENT_KEY = "instanceId";
    private static final String SET_NOTIFIED_KEY = "setNotified";

    private TextView mShowInstanceName;
    private RecyclerView mShowInstanceList;
    private TextView mShowInstanceDetails;
    private CheckBox mCheckBox;

    private ImageView mShowInstanceEdit;

    public static Intent getIntent(int taskId, Date scheduleDate, Integer scheduleCustomTimeId, HourMinute scheduleHourMinute, Context context) {
        Assert.assertTrue(scheduleDate != null);
        Assert.assertTrue((scheduleCustomTimeId == null) != (scheduleHourMinute == null));

        Intent intent = new Intent(context, ShowInstanceActivity.class);
        intent.putExtra(INTENT_KEY, NewInstanceData.getBundle(taskId, scheduleDate, scheduleCustomTimeId, scheduleHourMinute));
        return intent;
    }

    public static Intent getNotificationIntent(int taskId, Date scheduleDate, Integer scheduleCustomTimeId, HourMinute scheduleHourMinute, Context context) {
        Intent intent = new Intent(context, ShowInstanceActivity.class);
        intent.putExtra(INTENT_KEY, NewInstanceData.getBundle(taskId, scheduleDate, scheduleCustomTimeId, scheduleHourMinute));
        intent.putExtra(SET_NOTIFIED_KEY, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private boolean mFirst = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_instance);

        if (savedInstanceState == null)
            mFirst = true;

        mShowInstanceName = (TextView) findViewById(R.id.show_instance_name);

        mShowInstanceDetails = (TextView) findViewById(R.id.show_instance_details);

        mShowInstanceList = (RecyclerView) findViewById(R.id.show_instance_list);
        mShowInstanceList.setLayoutManager(new LinearLayoutManager(this));

        mCheckBox = (CheckBox) findViewById(R.id.show_instance_checkbox);

        mShowInstanceEdit = (ImageView) findViewById(R.id.show_instance_edit);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<ShowInstanceLoader.Data> onCreateLoader(int id, Bundle args) {
        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INTENT_KEY));
        Bundle bundle = intent.getParcelableExtra(INTENT_KEY);

        int taskId = NewInstanceData.getTaskId(bundle);

        Date date = NewInstanceData.getScheduleDate(bundle);
        Assert.assertTrue(date != null);

        Integer customTimeId = NewInstanceData.getScheduleCustomTimeId(bundle);
        HourMinute hourMinute = NewInstanceData.getScheduleHourMinute(bundle);

        if (customTimeId != null) {
            Assert.assertTrue(hourMinute == null);
            return new ShowInstanceLoader(this, taskId, date, customTimeId);
        } else {
            Assert.assertTrue(hourMinute != null);
            return new ShowInstanceLoader(this, taskId, date, hourMinute);
        }
    }

    @Override
    public void onLoadFinished(Loader<ShowInstanceLoader.Data> loader, final ShowInstanceLoader.Data data) {
        Intent intent = getIntent();

        if (intent.getBooleanExtra(SET_NOTIFIED_KEY, false) && mFirst) {
            mFirst = false;
            DomainFactory.getDomainFactory(this).setInstanceNotifiedNotShown(data.DataId, data.TaskId, data.ScheduleDate, data.ScheduleCustomTimeId, data.ScheduleHourMinute);
        }

        mShowInstanceName.setText(data.Name);

        mCheckBox.setChecked(data.Done);

        String scheduleText = data.DisplayText;
        if (TextUtils.isEmpty(scheduleText))
            mShowInstanceDetails.setVisibility(View.GONE);
        else
            mShowInstanceDetails.setText(scheduleText);

        if (data.HasChildren) {
            ArrayList<InstanceAdapter.Data> datas = new ArrayList<>();
            for (ShowInstanceLoader.InstanceData instanceData : data.InstanceDatas)
                datas.add(new InstanceAdapter.Data(instanceData.Done, instanceData.Name, instanceData.HasChildren, instanceData.TaskId, instanceData.ScheduleDate, instanceData.ScheduleCustomTimeId, instanceData.ScheduleHourMinute, null));

            mShowInstanceList.setAdapter(new InstanceAdapter(this, data.DataId, datas));
        }

        mCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = mCheckBox.isChecked();

                DomainFactory.getDomainFactory(ShowInstanceActivity.this).setInstanceDone(data.DataId, ShowInstanceActivity.this, data.TaskId, data.ScheduleDate, data.ScheduleCustomTimeId, data.ScheduleHourMinute, isChecked);
                data.Done = isChecked;

                TickService.startService(ShowInstanceActivity.this);
            }
        });

        mShowInstanceEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = EditInstanceActivity.getIntent(ShowInstanceActivity.this, data.TaskId, data.ScheduleDate, data.ScheduleCustomTimeId, data.ScheduleHourMinute);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<ShowInstanceLoader.Data> loader) {
    }
}
