package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.domainmodel.DomainLoader;
import com.example.krystianwsul.organizator.domainmodel.Instance;
import com.example.krystianwsul.organizator.utils.time.DateTime;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.NormalTime;
import com.example.krystianwsul.organizator.utils.time.Time;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowGroupActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<DomainFactory> {
    //private DomainFactory mDomainFactory;

    private RecyclerView mShowGroupList;
    private TimeStamp mTimeStamp;
    private TextView mShowGroupName;

    private static final String TIME_KEY = "time";

    public static Intent getIntent(Group group, Context context) {
        Intent intent = new Intent(context, ShowGroupActivity.class);
        intent.putExtra(TIME_KEY, group.getTimeStamp().getLong());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_group);

        Intent intent = getIntent();
        Assert.assertTrue(intent.hasExtra(TIME_KEY));
        long time = intent.getLongExtra(TIME_KEY, -1);
        Assert.assertTrue(time != -1);
        mTimeStamp = new TimeStamp(time);

        mShowGroupName = (TextView) findViewById(R.id.show_group_name);

        mShowGroupList = (RecyclerView) findViewById(R.id.show_group_list);
        mShowGroupList.setLayoutManager(new LinearLayoutManager(this));

        getSupportLoaderManager().initLoader(0, null, this);
    }

    private String getDisplayText(DomainFactory domainFactory, Instance instance) {
        Assert.assertTrue(domainFactory != null);
        Assert.assertTrue(instance != null);

        Time time = getTime(domainFactory, instance.getInstanceDateTime());
        Assert.assertTrue(time != null);
        return new DateTime(instance.getScheduleDate(), time).getDisplayText(this);
    }

    private Time getTime(DomainFactory domainFactory, DateTime dateTime) {
        Assert.assertTrue(domainFactory != null);
        Assert.assertTrue(dateTime != null);

        DayOfWeek dayOfWeek = dateTime.getDate().getDayOfWeek();
        HourMinute hourMinute = dateTime.getTime().getHourMinute(dayOfWeek);
        Time time = domainFactory.getCustomTimeFactory().getCustomTime(dayOfWeek, hourMinute);
        if (time == null)
            time = new NormalTime(hourMinute);
        return time;
    }

    @Override
    public Loader<DomainFactory> onCreateLoader(int id, Bundle args) {
        return new DomainLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<DomainFactory> loader, DomainFactory domainFactory) {
        ArrayList<Instance> instances = domainFactory.getInstanceFactory().getCurrentInstances(mTimeStamp);
        Assert.assertTrue(!instances.isEmpty());
        if (instances.size() == 1)
            finish();

        mShowGroupName.setText(getDisplayText(domainFactory, instances.get(0)));

        mShowGroupList.setAdapter(new InstanceAdapter(this, instances, false, domainFactory));
    }

    @Override
    public void onLoaderReset(Loader<DomainFactory> loader) {
        mShowGroupList.setAdapter(null);
    }
}