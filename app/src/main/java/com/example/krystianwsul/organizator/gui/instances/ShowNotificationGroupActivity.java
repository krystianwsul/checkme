package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.instances.Instance;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ShowNotificationGroupActivity extends AppCompatActivity {
    private RecyclerView mShowNotificationGroupList;
    private ArrayList<Instance> mInstances;

    private static final String INSTANCES_KEY = "instances";

    public static Intent getIntent(Context context, ArrayList<Instance> instances) {
        Intent intent = new Intent(context, ShowNotificationGroupActivity.class);

        ArrayList<Bundle> bundles = new ArrayList<>();
        for (Instance instance : instances)
            bundles.add(InstanceData.getBundle(instance));

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putParcelableArrayListExtra(INSTANCES_KEY, bundles);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_notification_group);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INSTANCES_KEY));
        ArrayList<Bundle> bundles = intent.getParcelableArrayListExtra(INSTANCES_KEY);
        Assert.assertTrue(bundles != null);
        Assert.assertTrue(!bundles.isEmpty());
        mInstances = new ArrayList<>();
        for (Bundle bundle : bundles)
            mInstances.add(InstanceData.getInstance(bundle));

        Collections.sort(mInstances, new Comparator<Instance>() {
            @Override
            public int compare(Instance lhs, Instance rhs) {
                return lhs.getInstanceDateTime().compareTo(rhs.getInstanceDateTime());
            }
        });

        mShowNotificationGroupList = (RecyclerView) findViewById(R.id.show_notification_group_list);
        mShowNotificationGroupList.setLayoutManager(new LinearLayoutManager(this));
        mShowNotificationGroupList.setAdapter(new InstanceAdapter(this, mInstances, true));
    }
}