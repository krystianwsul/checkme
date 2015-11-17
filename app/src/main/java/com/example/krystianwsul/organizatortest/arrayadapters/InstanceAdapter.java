package com.example.krystianwsul.organizatortest.arrayadapters;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
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
        final Instance instance = mInstances.get(position);

        if (convertView == null)  {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.show_instance_row, parent, false);

            InstanceHolder instanceHolder = new InstanceHolder();
            instanceHolder.instanceRowName = (TextView) convertView.findViewById(R.id.instance_row_name);
            instanceHolder.instanceRowImg = (ImageView) convertView.findViewById(R.id.instance_row_img);
            instanceHolder.instanceRowCheckBox = (CheckBox) convertView.findViewById(R.id.instance_row_checkbox);

            convertView.setTag(instanceHolder);
        }

        InstanceHolder instanceHolder = (InstanceHolder) convertView.getTag();

        instanceHolder.instanceRowName.setText(instance.getName());

        Resources resources = mContext.getResources();

        if (instance.getChildInstances().isEmpty())
            instanceHolder.instanceRowImg.setBackground(resources.getDrawable(R.drawable.ic_label_outline_black_18dp));
        else
            instanceHolder.instanceRowImg.setBackground(resources.getDrawable(R.drawable.ic_list_black_18dp));

        instanceHolder.instanceRowCheckBox.setChecked(instance.getDone() != null);
        instanceHolder.instanceRowCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                instance.setDone(isChecked);
            }
        });

        return convertView;
    }

    private class InstanceHolder {
        public TextView instanceRowName;
        public ImageView instanceRowImg;
        public CheckBox instanceRowCheckBox;
    }
}
