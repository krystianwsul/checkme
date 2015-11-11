package com.example.krystianwsul.organizatortest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.groups.Group;
import com.example.krystianwsul.organizatortest.domainmodel.groups.GroupFactory;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowGroup extends AppCompatActivity {
    public static final String INTENT_KEY = "groupLong";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_group);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INTENT_KEY));
        long groupLong = intent.getLongExtra(INTENT_KEY, -1);
        Assert.assertTrue(groupLong != -1);
        TimeStamp timeStamp = new TimeStamp(groupLong);
        Group group = GroupFactory.getInstance().getGroup(timeStamp);
        Assert.assertTrue(group != null);

        TextView showGroupName = (TextView) findViewById(R.id.show_group_name);
        showGroupName.setText(group.getNameText(this));

        ListView showGroupList = (ListView) findViewById(R.id.show_group_list);
        Assert.assertTrue(!group.singleInstance());
        showGroupList.setAdapter(new InstanceAdapter(this, group.getInstances()));

        showGroupList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Instance instance = (Instance) parent.getItemAtPosition(position);
                Intent intent = new Intent(view.getContext(), ShowInstance.class);
                intent.putExtra(instance.getIntentKey(), instance.getIntentValue());
                startActivity(intent);
            }
        });
    }
}
