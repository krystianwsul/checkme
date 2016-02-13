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
import com.example.krystianwsul.organizator.domainmodel.Instance;
import com.example.krystianwsul.organizator.loaders.DomainLoader;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowInstanceActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<DomainFactory> {
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

    public static Intent getNotificationIntent(Instance instance, Context context) {
        Intent intent = new Intent(context, ShowInstanceActivity.class);
        intent.putExtra(INTENT_KEY, InstanceData.getBundle(instance));
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
    public Loader<DomainFactory> onCreateLoader(int id, Bundle args) {
        return new DomainLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<DomainFactory> loader, final DomainFactory domainFactory) {
        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INTENT_KEY));
        Bundle bundle = intent.getParcelableExtra(INTENT_KEY);
        final Instance instance = InstanceData.getInstance(domainFactory, bundle);
        Assert.assertTrue(instance != null);

        if (intent.getBooleanExtra(SET_NOTIFIED_KEY, false) && mFirst) {
            mFirst = false;

            domainFactory.setInstanceNotifiedNotShown(instance);

            domainFactory.save();
        }

        mShowInstanceName.setText(instance.getName());

        mCheckBox.setChecked(instance.getDone() != null);

        String scheduleText = instance.getDisplayText(this);
        if (TextUtils.isEmpty(scheduleText))
            mShowInstanceDetails.setVisibility(View.GONE);
        else
            mShowInstanceDetails.setText(scheduleText);

        if (!instance.getChildInstances().isEmpty())
            mShowInstanceList.setAdapter(new InstanceAdapter(this, new ArrayList<>(instance.getChildInstances()), false, domainFactory));

        mCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = mCheckBox.isChecked();

                domainFactory.setInstanceDone(ShowInstanceActivity.this, instance, isChecked);

                domainFactory.save();

                TickService.startService(ShowInstanceActivity.this);
            }
        });

        mShowInstanceEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = EditInstanceActivity.getIntent(instance, ShowInstanceActivity.this);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<DomainFactory> loader) {
        mCheckBox.setOnClickListener(null);
        mShowInstanceList.setAdapter(null);
        mShowInstanceEdit.setOnClickListener(null);
    }
}
