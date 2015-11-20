package com.example.krystianwsul.organizatortest.arrayadapters;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.ShowInstanceActivity;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 11/11/2015.
 */
public class InstanceAdapter extends RecyclerView.Adapter<InstanceAdapter.InstanceHolder> {
    private final Context mContext;
    private final ArrayList<Instance> mInstances;

    public InstanceAdapter(Context context, ArrayList<Instance> instances) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instances != null);
        Assert.assertTrue(!instances.isEmpty());

        mContext = context;
        mInstances = instances;
    }

    @Override
    public InstanceHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TableLayout instanceRow = (TableLayout) LayoutInflater.from(mContext).inflate(R.layout.show_instance_row, parent, false);

        TextView instanceRowName = (TextView) instanceRow.findViewById(R.id.instance_row_name);
        ImageView instanceRowImg = (ImageView) instanceRow.findViewById(R.id.instance_row_img);
        CheckBox instanceRowCheckBox = (CheckBox) instanceRow.findViewById(R.id.instance_row_checkbox);

        return new InstanceHolder(instanceRow, instanceRowName, instanceRowImg, instanceRowCheckBox);
    }

    @Override
    public void onBindViewHolder(InstanceHolder instanceHolder, int position) {
        final Instance instance = mInstances.get(position);

        instanceHolder.mInstanceRowName.setText(instance.getName());

        Resources resources = mContext.getResources();

        if (instance.getChildInstances().isEmpty())
            instanceHolder.mInstanceRowImg.setBackground(resources.getDrawable(R.drawable.ic_label_outline_black_18dp));
        else
            instanceHolder.mInstanceRowImg.setBackground(resources.getDrawable(R.drawable.ic_list_black_18dp));

        instanceHolder.mInstanceRowCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v;
                boolean isChecked = checkBox.isChecked();

                instance.setDone(isChecked);
            }
        });

        instanceHolder.mInstanceRowCheckBox.setChecked(instance.getDone() != null);

        instanceHolder.mInstanceRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContext.startActivity(ShowInstanceActivity.getIntent(instance, mContext));
            }
        });
    }

    @Override
    public int getItemCount() {
        return mInstances.size();
    }

    public class InstanceHolder extends RecyclerView.ViewHolder {
        public final TableLayout mInstanceRow;
        public final TextView mInstanceRowName;
        public final ImageView mInstanceRowImg;
        public final CheckBox mInstanceRowCheckBox;

        public InstanceHolder(TableLayout instanceRow, TextView instanceRowName, ImageView instanceRowImg, CheckBox instanceRowCheckBox) {
            super(instanceRow);

            Assert.assertTrue(instanceRow != null);
            Assert.assertTrue(instanceRowName != null);
            Assert.assertTrue(instanceRowImg != null);
            Assert.assertTrue(instanceRowCheckBox != null);

            mInstanceRow = instanceRow;
            mInstanceRowName = instanceRowName;
            mInstanceRowImg = instanceRowImg;
            mInstanceRowCheckBox = instanceRowCheckBox;
        }
    }
}
