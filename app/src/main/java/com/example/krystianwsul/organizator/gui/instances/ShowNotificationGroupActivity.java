package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.domainmodel.Instance;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ShowNotificationGroupActivity extends AppCompatActivity {
    private static final String INSTANCES_KEY = "instances";
    private static final String SET_NOTIFIED_KEY = "setNotified";

    public static Intent getShowIntent(Context context, ArrayList<Instance> instances) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instances != null);
        Assert.assertTrue(!instances.isEmpty());

        ArrayList<Bundle> bundles = new ArrayList<>();
        for (Instance instance : instances)
            bundles.add(InstanceData.getBundle(instance));

        return getIntent(context, bundles);
    }

    private static Intent getIntent(Context context, ArrayList<Bundle> bundles) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(bundles != null);
        Assert.assertTrue(!bundles.isEmpty());

        Intent intent = new Intent(context, ShowNotificationGroupActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putParcelableArrayListExtra(INSTANCES_KEY, bundles);

        return intent;
    }


    public static Intent getNotificationIntent(Context context, ArrayList<Bundle> bundles) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(bundles != null);
        Assert.assertTrue(!bundles.isEmpty());

        Intent intent = getIntent(context, bundles);
        intent.putExtra(SET_NOTIFIED_KEY, true);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_notification_group);

        DomainFactory domainFactory = DomainFactory.getDomainFactory(this);
        Assert.assertTrue(domainFactory != null);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(INSTANCES_KEY));
        ArrayList<Bundle> bundles = intent.getParcelableArrayListExtra(INSTANCES_KEY);
        Assert.assertTrue(bundles != null);
        Assert.assertTrue(!bundles.isEmpty());

        boolean setNotified = intent.getBooleanExtra(SET_NOTIFIED_KEY, false);

        ArrayList<Instance> instances = new ArrayList<>();
        for (Bundle bundle : bundles) {
            Instance instance = InstanceData.getInstance(domainFactory, bundle);
            Assert.assertTrue(instance != null);

            if (savedInstanceState == null && setNotified)
                instance.setNotified();

            instances.add(instance);
        }

        if (savedInstanceState == null && setNotified)
            domainFactory.save();

        Collections.sort(instances, new Comparator<Instance>() {
            @Override
            public int compare(Instance lhs, Instance rhs) {
                return lhs.getInstanceDateTime().compareTo(rhs.getInstanceDateTime());
            }
        });

        RecyclerView showNotificationGroupList = (RecyclerView) findViewById(R.id.show_notification_group_list);
        showNotificationGroupList.setLayoutManager(new LinearLayoutManager(this));
        showNotificationGroupList.setAdapter(new InstanceAdapter(this, instances, true, domainFactory));
    }
}