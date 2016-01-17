package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowInstanceActivity extends AppCompatActivity {
    private TextView mShowInstanceName;
    private RecyclerView mShowInstanceList;
    private TextView mShowInstanceDetails;
    private CheckBox mCheckBox;

    private Instance mInstance;

    private static final String INTENT_KEY = "instanceId";

    public static Intent getIntent(Instance instance, Context context) {
        Intent intent = new Intent(context, ShowInstanceActivity.class);
        intent.putExtra(INTENT_KEY, InstanceData.getBundle(instance));
        return intent;
    }

    public static Intent getNotificationIntent(Instance instance, Context context) {
        Intent intent = new Intent(context, ShowInstanceActivity.class);
        intent.putExtra(INTENT_KEY, InstanceData.getBundle(instance));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_instance);

        mShowInstanceName = (TextView) findViewById(R.id.show_instance_name);

        mShowInstanceDetails = (TextView) findViewById(R.id.show_instance_details);

        mShowInstanceList = (RecyclerView) findViewById(R.id.show_instance_list);
        mShowInstanceList.setLayoutManager(new LinearLayoutManager(this));

        mCheckBox = (CheckBox) findViewById(R.id.show_instance_checkbox);
        mCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = mCheckBox.isChecked();
                mInstance.setDone(isChecked, ShowInstanceActivity.this);
            }
        });

        ImageView showInstanceEdit = (ImageView) findViewById(R.id.show_instance_edit);
        showInstanceEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = EditInstanceActivity.getIntent(mInstance, ShowInstanceActivity.this);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        DomainFactory domainFactory = DomainFactory.getDomainFactory(this);
        Assert.assertTrue(domainFactory != null);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INTENT_KEY));
        Bundle bundle = intent.getParcelableExtra(INTENT_KEY);
        mInstance = InstanceData.getInstance(domainFactory, bundle);
        Assert.assertTrue(mInstance != null);

        mShowInstanceName.setText(mInstance.getName());

        mCheckBox.setChecked(mInstance.getDone() != null);

        String scheduleText = mInstance.getDisplayText(this);
        if (TextUtils.isEmpty(scheduleText))
            mShowInstanceDetails.setVisibility(View.GONE);
        else
            mShowInstanceDetails.setText(scheduleText);

        if (!mInstance.getChildInstances().isEmpty())
            mShowInstanceList.setAdapter(new InstanceAdapter(this, new ArrayList<>(mInstance.getChildInstances()), false));
    }
}
