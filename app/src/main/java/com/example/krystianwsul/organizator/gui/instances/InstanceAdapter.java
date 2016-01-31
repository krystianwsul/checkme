package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.domainmodel.Instance;
import com.example.krystianwsul.organizator.notifications.TickService;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class InstanceAdapter extends RecyclerView.Adapter<InstanceAdapter.InstanceHolder> {
    private final Context mContext;

    private final ArrayList<Instance> mDoneInstances = new ArrayList<>();
    private final ArrayList<Instance> mNotDoneInstances = new ArrayList<>();
    private final boolean mShowDetails;

    private final DomainFactory mDomainFactory;

    private final static Comparator<Instance> sComparator = new Comparator<Instance>() {
        @Override
        public int compare(Instance lhs, Instance rhs) {
            Assert.assertTrue(lhs.getDone() != null);
            Assert.assertTrue(rhs.getDone() != null);

            return lhs.getDone().compareTo(rhs.getDone());
        }
    };

    public InstanceAdapter(Context context, ArrayList<Instance> instances, boolean showDetails, DomainFactory domainFactory) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(instances != null);
        Assert.assertTrue(!instances.isEmpty());
        Assert.assertTrue(domainFactory != null);

        mContext = context;
        mShowDetails = showDetails;

        mDomainFactory = domainFactory;

        for (Instance instance : instances) {
            if (instance.getDone() != null)
                mDoneInstances.add(instance);
            else
                mNotDoneInstances.add(instance);
        }

        sort();
    }

    private void sort() {
        Collections.sort(mDoneInstances, sComparator);
    }

    private Instance getInstance(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < mDoneInstances.size() + mNotDoneInstances.size());

        if (position < mDoneInstances.size())
            return mDoneInstances.get(position);
        else
            return mNotDoneInstances.get(position - mDoneInstances.size());
    }

    @Override
    public InstanceHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TableLayout instanceRow = (TableLayout) LayoutInflater.from(mContext).inflate(R.layout.show_instance_row, parent, false);

        TextView instanceRowName = (TextView) instanceRow.findViewById(R.id.instance_row_name);
        TextView instanceRowDetails = (TextView) instanceRow.findViewById(R.id.instance_row_details);
        ImageView instanceRowImg = (ImageView) instanceRow.findViewById(R.id.instance_row_img);
        CheckBox instanceRowCheckBox = (CheckBox) instanceRow.findViewById(R.id.instance_row_checkbox);

        return new InstanceHolder(instanceRow, instanceRowName, instanceRowDetails, instanceRowImg, instanceRowCheckBox);
    }

    @Override
    public void onBindViewHolder(final InstanceHolder instanceHolder, int position) {
        Instance instance = getInstance(position);

        instanceHolder.mInstanceRowName.setText(instance.getName());
        if (mShowDetails)
            instanceHolder.mInstanceRowDetails.setText(instance.getDisplayText(mContext));
        else
            instanceHolder.mInstanceRowDetails.setVisibility(View.GONE);

        if (instance.getChildInstances().isEmpty())
            instanceHolder.mInstanceRowImg.setBackground(ContextCompat.getDrawable(mContext, R.drawable.ic_label_outline_black_24dp));
        else
            instanceHolder.mInstanceRowImg.setBackground(ContextCompat.getDrawable(mContext, R.drawable.ic_list_black_24dp));

        instanceHolder.mInstanceRowCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                instanceHolder.onCheckBoxClick((CheckBox) v);
            }
        });

        instanceHolder.mInstanceRowCheckBox.setChecked(instance.getDone() != null);

        instanceHolder.mInstanceRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                instanceHolder.onRowClick();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDoneInstances.size() + mNotDoneInstances.size();
    }

    private int indexOf(Instance instance) {
        Assert.assertTrue(instance != null);

        if (mDoneInstances.contains(instance)) {
            return mDoneInstances.indexOf(instance);
        } else {
            Assert.assertTrue(mNotDoneInstances.contains(instance));
            return mDoneInstances.size() + mNotDoneInstances.indexOf(instance);
        }
    }

    public class InstanceHolder extends RecyclerView.ViewHolder {
        public final TableLayout mInstanceRow;
        public final TextView mInstanceRowName;
        public final TextView mInstanceRowDetails;
        public final ImageView mInstanceRowImg;
        public final CheckBox mInstanceRowCheckBox;

        public InstanceHolder(TableLayout instanceRow, TextView instanceRowName, TextView instanceRowDetails, ImageView instanceRowImg, CheckBox instanceRowCheckBox) {
            super(instanceRow);

            Assert.assertTrue(instanceRowName != null);
            Assert.assertTrue(instanceRowDetails != null);
            Assert.assertTrue(instanceRowImg != null);
            Assert.assertTrue(instanceRowCheckBox != null);

            mInstanceRow = instanceRow;
            mInstanceRowName = instanceRowName;
            mInstanceRowDetails = instanceRowDetails;
            mInstanceRowImg = instanceRowImg;
            mInstanceRowCheckBox = instanceRowCheckBox;
        }

        public void onCheckBoxClick(CheckBox checkBox) {
            Assert.assertTrue(checkBox != null);

            int position = getAdapterPosition();
            Instance instance = getInstance(position);
            Assert.assertTrue(instance != null);

            boolean isChecked = checkBox.isChecked();
            instance.setDone(isChecked, mContext);

            mDomainFactory.save();

            TickService.startService(mContext);

            if (isChecked) {
                Assert.assertTrue(mNotDoneInstances.contains(instance));

                int oldPosition = indexOf(instance);

                mNotDoneInstances.remove(instance);
                mDoneInstances.add(instance);
                sort();

                int newPosition = indexOf(instance);

                notifyItemMoved(oldPosition, newPosition);
            } else {
                Assert.assertTrue(mDoneInstances.contains(instance));

                int oldPosition = indexOf(instance);

                mDoneInstances.remove(instance);
                mNotDoneInstances.add(instance);

                int newPosition = indexOf(instance);

                notifyItemMoved(oldPosition, newPosition);
            }
        }

        public void onRowClick() {
            Instance instance = getInstance(getAdapterPosition());
            Assert.assertTrue(instance != null);

            mContext.startActivity(ShowInstanceActivity.getIntent(instance, mContext));
        }
    }
}
