package com.example.krystianwsul.organizatortest.adapters;

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
        LayoutInflater inflater = LayoutInflater.from(mContext);
        Instance instance = mInstances.get(position);

        View rowView = inflater.inflate(R.layout.show_task_row, parent, false);

        TextView rowName = (TextView) rowView.findViewById(R.id.task_row_name);
        rowName.setText(instance.getName());

        TextView rowDetails = (TextView) rowView.findViewById(R.id.task_row_details);
        rowDetails.setVisibility(View.GONE);

        Resources resources = mContext.getResources();

        ImageView imgList = (ImageView) rowView.findViewById(R.id.task_row_img);
        if (instance.getChildInstances().isEmpty())
            imgList.setBackground(resources.getDrawable(R.drawable.ic_label_outline_black_18dp));
        else
            imgList.setBackground(resources.getDrawable(R.drawable.ic_list_black_18dp));

        return rowView;
    }
}
