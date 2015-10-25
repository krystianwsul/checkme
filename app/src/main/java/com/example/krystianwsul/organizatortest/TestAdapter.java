package com.example.krystianwsul.organizatortest;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.domain.instances.TopInstance;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/23/2015.
 */
public class TestAdapter extends ArrayAdapter<TopInstance> {
    private final Activity mActivity;
    private final ArrayList<TopInstance> mInstances;

    public TestAdapter(Activity activity, ArrayList<TopInstance> instances) {
        super(activity, -1, instances);

        Assert.assertTrue(activity != null);
        Assert.assertTrue(instances != null);
        Assert.assertTrue(!instances.isEmpty());

        mActivity = activity;
        mInstances = instances;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.show_tasks_row, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.tasks_row_label);
        TopInstance instance = mInstances.get(position);
        textView.setText(instance.getTask().getName() + " " + instance.getDateTime().getDate().getDayOfWeek().toString() + ", " + instance.getDateTime().toString());
        return rowView;
    }
}
