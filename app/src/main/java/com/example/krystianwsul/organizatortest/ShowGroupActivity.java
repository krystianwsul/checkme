package com.example.krystianwsul.organizatortest;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.groups.Group;
import com.example.krystianwsul.organizatortest.domainmodel.groups.GroupFactory;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;

import junit.framework.Assert;

public class ShowGroupActivity extends AppCompatActivity {
    private static final String INTENT_KEY = "groupLong";

    public static Intent getIntent(Group group, Context context) {
        Intent intent = new Intent(context, ShowGroupActivity.class);
        intent.putExtra(ShowGroupActivity.INTENT_KEY, group.getTimeStamp().getLong());
        return intent;
    }

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
                startActivity(ShowInstanceActivity.getIntent(instance, view.getContext()));
            }
        });
    }
}
