package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.loaders.ShowInstanceLoader;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.InstanceKey;

import junit.framework.Assert;

public class ShowInstanceActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ShowInstanceLoader.Data> {
    private static final String INSTANCE_KEY = "instanceKey";
    private static final String SET_NOTIFIED_KEY = "setNotified";

    private TextView mShowInstanceName;
    private InstanceListFragment mShowInstanceList;
    private TextView mShowInstanceDetails;
    private CheckBox mCheckBox;

    private ImageView mShowInstanceEdit;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_instance);

        if (savedInstanceState == null)
            mFirst = true;

        mShowInstanceName = (TextView) findViewById(R.id.show_instance_name);

        mShowInstanceDetails = (TextView) findViewById(R.id.show_instance_details);

        mShowInstanceList = (InstanceListFragment) getSupportFragmentManager().findFragmentById(R.id.show_instance_list);

        mCheckBox = (CheckBox) findViewById(R.id.show_instance_checkbox);

        mShowInstanceEdit = (ImageView) findViewById(R.id.show_instance_edit);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<ShowInstanceLoader.Data> onCreateLoader(int id, Bundle args) {
        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INSTANCE_KEY));
        InstanceKey instanceKey = intent.getParcelableExtra(INSTANCE_KEY);

        return new ShowInstanceLoader(this, instanceKey);
    }

    @Override
    public void onLoadFinished(Loader<ShowInstanceLoader.Data> loader, final ShowInstanceLoader.Data data) {
        Intent intent = getIntent();

        Log.e("asdf", "onLoadFinished");

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

        if (data.HasChildren)
            mShowInstanceList.setAdapter(data.DataId, data.InstanceAdapterDatas.values());

        mCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = mCheckBox.isChecked();

                DomainFactory.getDomainFactory(ShowInstanceActivity.this).setInstanceDone(data.DataId, data.InstanceKey, isChecked);

                TickService.startService(ShowInstanceActivity.this);

                data.Done = isChecked;
            }
        });

        mShowInstanceEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = EditInstanceActivity.getIntent(ShowInstanceActivity.this, data.InstanceKey);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<ShowInstanceLoader.Data> loader) {
    }
}
