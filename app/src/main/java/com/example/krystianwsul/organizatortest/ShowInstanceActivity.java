package com.example.krystianwsul.organizatortest;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.arrayadapters.InstanceAdapter;
import com.example.krystianwsul.organizatortest.domainmodel.instances.DailyInstance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.InstanceFactory;
import com.example.krystianwsul.organizatortest.domainmodel.instances.SingleInstance;
import com.example.krystianwsul.organizatortest.domainmodel.instances.WeeklyInstance;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowInstanceActivity extends AppCompatActivity {
    private static final String INTENT_KEY = "instanceId";

    public static Intent getIntent(Instance instance, Context context) {
        Intent intent = new Intent(context, ShowInstanceActivity.class);
        intent.putExtra(INTENT_KEY, instance.getId());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_instance);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INTENT_KEY));
        int instanceId = intent.getIntExtra(INTENT_KEY, -1);
        Assert.assertTrue(instanceId != -1);
        final Instance instance = InstanceFactory.getInstance().getInstance(instanceId);
        Assert.assertTrue(instance != null);

        TextView showInstanceName = (TextView) findViewById(R.id.show_instance_name);
        showInstanceName.setText(instance.getName());

        TextView showInstanceDetails = (TextView) findViewById(R.id.show_instance_details);
        String scheduleText = instance.getScheduleText(this);
        if (TextUtils.isEmpty(scheduleText))
            showInstanceDetails.setVisibility(View.GONE);
        else
            showInstanceDetails.setText(scheduleText);

        RecyclerView showInstanceList = (RecyclerView) findViewById(R.id.show_instance_list);
        if (instance.getChildInstances().isEmpty()) {
            showInstanceList.setVisibility(View.GONE);
        } else {
            showInstanceList.setLayoutManager(new LinearLayoutManager(this));
            showInstanceList.setAdapter(new InstanceAdapter(this, new ArrayList(instance.getChildInstances())));
        }

        final CheckBox checkBox = (CheckBox) findViewById(R.id.show_instance_checkbox);
        checkBox.setChecked(instance.getDone() != null);
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = checkBox.isChecked();
                instance.setDone(isChecked);
            }
        });
    }
}
