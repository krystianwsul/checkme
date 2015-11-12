package com.example.krystianwsul.organizatortest.arrayadapters;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 11/11/2015.
 */
public class InstanceAdapter extends ArrayAdapter<Instance> {
    private final Context mContext;
    private final ArrayList<Instance> mInstances;

    public InstanceAdapter(Context context, ArrayList<Instance> instances) {
        super(context, -1, instances);

        Assert.assertTrue(context != null);
        Assert.assertTrue(instances != null);
        Assert.assertTrue(!instances.isEmpty());

        mContext = context;
        mInstances = instances;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Instance instance = mInstances.get(position);

        if (convertView == null)  {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.show_task_row, parent, false);

            InstanceHolder instanceHolder = new InstanceHolder();
            instanceHolder.taskRowName = (TextView) convertView.findViewById(R.id.task_row_name);
            instanceHolder.taskRowDetails = (TextView) convertView.findViewById(R.id.task_row_details);
            instanceHolder.taskRowImg = (ImageView) convertView.findViewById(R.id.task_row_img);

            convertView.setTag(instanceHolder);
        }

        InstanceHolder instanceHolder = (InstanceHolder) convertView.getTag();

        instanceHolder.taskRowName.setText(instance.getName());

        instanceHolder.taskRowDetails.setVisibility(View.GONE);

        Resources resources = mContext.getResources();

        if (instance.getChildInstances().isEmpty())
            instanceHolder.taskRowImg.setBackground(resources.getDrawable(R.drawable.ic_label_outline_black_18dp));
        else
            instanceHolder.taskRowImg.setBackground(resources.getDrawable(R.drawable.ic_list_black_18dp));

        return convertView;
    }

    private class InstanceHolder {
        public TextView taskRowName;
        public TextView taskRowDetails;
        public ImageView taskRowImg;
    }
}
