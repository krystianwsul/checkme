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
import com.example.krystianwsul.organizator.loaders.ShowGroupLoader;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowGroupActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ShowGroupLoader.Data> {
    private RecyclerView mShowGroupList;
    private TimeStamp mTimeStamp;
    private TextView mShowGroupName;

    private static final String TIME_KEY = "time";

    public static Intent getIntent(GroupListFragment.Group group, Context context) {
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

    @Override
    public Loader<ShowGroupLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ShowGroupLoader(this, mTimeStamp);
    }

    @Override
    public void onLoadFinished(Loader<ShowGroupLoader.Data> loader, ShowGroupLoader.Data data) {
        mShowGroupName.setText(data.DisplayText);

        ArrayList<InstanceAdapter.Data> datas = new ArrayList<>();
        for (ShowGroupLoader.InstanceData instanceData : data.InstanceDatas)
            datas.add(new InstanceAdapter.Data(instanceData.Done, instanceData.Name, instanceData.HasChildren, instanceData.InstanceKey, null));

        mShowGroupList.setAdapter(new InstanceAdapter(this, data.DataId, datas));
    }

    @Override
    public void onLoaderReset(Loader<ShowGroupLoader.Data> loader) {
        mShowGroupList.setAdapter(null);
    }
}